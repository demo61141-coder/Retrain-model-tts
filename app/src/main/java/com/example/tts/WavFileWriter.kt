package com.example.tts

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object WavFileWriter {

    const val SAMPLE_RATE = 22050
    private const val CHANNELS = 1
    private const val BITS_PER_SAMPLE = 16

    /**
     * Writes 16-bit mono PCM WAV file at 22050 Hz from normalized float samples (-1.0f..1.0f).
     */
    fun writeMono16BitWav(
        samples: FloatArray,
        sampleRate: Int = SAMPLE_RATE,
        outputFile: File
    ) {
        outputFile.parentFile?.mkdirs()
        val numSamples = samples.size
        val byteRate = sampleRate * CHANNELS * (BITS_PER_SAMPLE / 8)
        val blockAlign = CHANNELS * (BITS_PER_SAMPLE / 8)
        val dataSize = numSamples * 2
        val totalChunkSize = 36 + dataSize

        FileOutputStream(outputFile).use { fos ->
            BufferedOutputStream(fos, 65536).use { out ->
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                // RIFF chunk descriptor
                header.put("RIFF".toByteArray(Charsets.US_ASCII))
                header.putInt(totalChunkSize)
                header.put("WAVE".toByteArray(Charsets.US_ASCII))
                // fmt sub-chunk
                header.put("fmt ".toByteArray(Charsets.US_ASCII))
                header.putInt(16) // PCM subchunk1 size
                header.putShort(1.toShort()) // AudioFormat 1 = PCM
                header.putShort(CHANNELS.toShort())
                header.putInt(sampleRate)
                header.putInt(byteRate)
                header.putShort(blockAlign.toShort())
                header.putShort(BITS_PER_SAMPLE.toShort())
                // data sub-chunk
                header.put("data".toByteArray(Charsets.US_ASCII))
                header.putInt(dataSize)
                out.write(header.array())

                val pcmBuffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until numSamples) {
                    val clamped = max(-1.0f, min(1.0f, samples[i]))
                    val pcm16 = (clamped * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort()
                    pcmBuffer.putShort(pcm16)
                    if (!pcmBuffer.hasRemaining()) {
                        out.write(pcmBuffer.array(), 0, pcmBuffer.position())
                        pcmBuffer.clear()
                    }
                }
                if (pcmBuffer.position() > 0) {
                    out.write(pcmBuffer.array(), 0, pcmBuffer.position())
                }
                out.flush()
            }
        }
    }

    /**
     * Computes a compact comma-separated string of normalized waveform peaks (0.08..1.0)
     * for rendering the Waveform UI in the Audio Player.
     */
    fun computeWaveformPeaks(samples: FloatArray, barCount: Int = 44): String {
        if (samples.isEmpty()) {
            return List(barCount) { "0.25" }.joinToString(",")
        }
        val bucketSize = max(1, samples.size / barCount)
        val rawPeaks = FloatArray(barCount)
        var maxPeak = 0.01f

        for (b in 0 until barCount) {
            val start = b * bucketSize
            val end = min(samples.size, start + bucketSize)
            if (start >= end) {
                rawPeaks[b] = 0.12f
                continue
            }
            var sumAbs = 0f
            var localMax = 0f
            val step = max(1, (end - start) / 64)
            var count = 0
            var idx = start
            while (idx < end) {
                val a = abs(samples[idx])
                sumAbs += a
                if (a > localMax) localMax = a
                count++
                idx += step
            }
            val avg = if (count > 0) sumAbs / count else 0f
            val combined = (localMax * 0.65f) + (avg * 0.35f)
            rawPeaks[b] = combined
            if (combined > maxPeak) maxPeak = combined
        }

        return rawPeaks.joinToString(",") { peak ->
            val normalized = (peak / maxPeak).coerceIn(0.10f, 1.0f)
            "%.2f".format(java.util.Locale.US, normalized)
        }
    }

    /**
     * Applies subtle pitch adjustment when user changes Pitch slider away from 1.0f (Normal).
     * When pitch == 1.0f, returns the exact unaltered sample array.
     */
    fun applyPitchIfNeeded(samples: FloatArray, pitch: Float): FloatArray {
        if (samples.isEmpty() || abs(pitch - 1.0f) < 0.02f) {
            return samples
        }
        val safePitch = pitch.coerceIn(0.75f, 1.35f)
        val targetLength = max(1, (samples.size / safePitch).roundToInt())
        val output = FloatArray(targetLength)
        for (i in 0 until targetLength) {
            val srcPos = i * safePitch
            val idx0 = srcPos.toInt().coerceIn(0, samples.lastIndex)
            val idx1 = (idx0 + 1).coerceIn(0, samples.lastIndex)
            val frac = srcPos - idx0
            output[i] = samples[idx0] * (1f - frac) + samples[idx1] * frac
        }
        return output
    }

    /**
     * Smooths each synthesized piece right before joining:
     * 1. Strips leading and trailing exact 0.0f samples.
     * 2. For real speech segments (>= 180ms), trims VITS cold-start pre-onset murmur/pops
     *    (stepping back 30ms before the first sustained syllable vowel so initial consonants
     *    like শ/স/হ are 100% preserved) and trims post-speech tail pops (keeping 60ms of natural decay).
     * 3. Applies a 20ms raised-cosine fade-in (0 -> 1) at the start and a 20ms raised-cosine fade-out (1 -> 0)
     *    at the end (clamped to half the piece length if the piece is very short so the two fades never overlap).
     */
    fun preparePieceForJoining(
        samples: FloatArray,
        sampleRate: Int = SAMPLE_RATE
    ): FloatArray {
        if (samples.isEmpty()) return samples

        var firstNonZero = 0
        while (firstNonZero < samples.size && samples[firstNonZero] == 0.0f) {
            firstNonZero++
        }
        if (firstNonZero >= samples.size) {
            return FloatArray(0)
        }

        var lastNonZero = samples.lastIndex
        while (lastNonZero > firstNonZero && samples[lastNonZero] == 0.0f) {
            lastNonZero--
        }

        var startIdx = firstNonZero
        var endIdx = lastNonZero + 1
        val nonZeroLen = endIdx - startIdx

        val winSize = (sampleRate * 0.015).roundToInt().coerceAtLeast(1) // 15ms window
        val numWins = nonZeroLen / winSize

        if (numWins >= 12) {
            val rms = FloatArray(numWins)
            var maxRms = 0f
            for (w in 0 until numWins) {
                val base = startIdx + w * winSize
                var sumSq = 0.0
                for (i in 0 until winSize) {
                    val v = samples[base + i].toDouble()
                    sumSq += v * v
                }
                val r = kotlin.math.sqrt(sumSq / winSize.toDouble()).toFloat()
                rms[w] = r
                if (r > maxRms) maxRms = r
            }

            if (maxRms >= 0.08f) {
                val onsetThresh = max(0.075f, maxRms * 0.22f)
                var firstWin = 0
                for (w in 0 until numWins) {
                    val nextR = if (w + 1 < numWins) rms[w + 1] else rms[w]
                    if (rms[w] >= onsetThresh && nextR >= onsetThresh * 0.85f) {
                        firstWin = w
                        break
                    }
                }

                // Check if this early onset is an isolated cold-start pop/murmur followed by >= 60ms (4 windows)
                // of near-silence before the first strong syllable (rms >= 0.34 * maxRms)
                val strongThresh = max(0.11f, maxRms * 0.34f)
                val silenceThresh = max(0.045f, maxRms * 0.10f)
                val searchLimit = min(numWins - 2, firstWin + 45)
                var gapStart = -1
                var silentCount = 0
                for (w in firstWin until searchLimit) {
                    if (silentCount == 0 && rms[w] >= strongThresh && rms[w + 1] >= strongThresh * 0.85f) {
                        // Reached strong sustained speech before any silence gap
                        break
                    }
                    if (rms[w] < silenceThresh) {
                        if (silentCount == 0) gapStart = w
                        silentCount++
                    } else if (silentCount >= 4) {
                        // Found speech after a >= 60ms silence gap; verify a strong syllable follows
                        var hasStrongAfter = false
                        val checkEnd = min(numWins, w + 16)
                        for (k in w until checkEnd) {
                            if (rms[k] >= onsetThresh) {
                                hasStrongAfter = true
                                break
                            }
                        }
                        if (hasStrongAfter && gapStart > firstWin) {
                            firstWin = w
                        }
                        break
                    } else {
                        silentCount = 0
                    }
                }

                // Step back 2 windows (30ms) to preserve initial consonant attack
                val trimStartWin = max(0, firstWin - 2)
                startIdx = firstNonZero + trimStartWin * winSize

                // Trim trailing post-silence <EOS>/<BLNK> pops from the end
                val tailThresh = max(0.040f, maxRms * 0.15f)
                var lastWin = numWins - 1
                for (w in (numWins - 1) downTo firstWin) {
                    val prevR = if (w > 0) rms[w - 1] else rms[w]
                    if (rms[w] >= tailThresh && prevR >= tailThresh * 0.80f) {
                        lastWin = w
                        break
                    }
                }
                // Keep 4 windows (60ms) of natural syllable release
                val trimEndSample = firstNonZero + (lastWin + 4) * winSize
                endIdx = min(endIdx, max(startIdx + winSize, trimEndSample))
            }
        }

        val result = samples.copyOfRange(startIdx, endIdx)
        val targetFadeSamples = (sampleRate * 0.020).roundToInt().coerceAtLeast(1)
        val fadeLen = min(targetFadeSamples, result.size / 2)

        if (fadeLen > 0) {
            val denom = max(1, fadeLen - 1).toDouble()
            for (i in 0 until fadeLen) {
                val progress = if (fadeLen > 1) i.toDouble() / denom else 0.0
                // Raised-cosine fade-in: 0.0 -> 1.0
                val fadeInGain = (0.5 * (1.0 - kotlin.math.cos(Math.PI * progress))).toFloat()
                result[i] = result[i] * fadeInGain

                // Raised-cosine fade-out: 1.0 -> 0.0
                val fadeOutGain = (0.5 * (1.0 + kotlin.math.cos(Math.PI * progress))).toFloat()
                val outIdx = result.size - fadeLen + i
                result[outIdx] = result[outIdx] * fadeOutGain
            }
        }

        return result
    }
}


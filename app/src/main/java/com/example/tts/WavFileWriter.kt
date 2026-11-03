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
     * 1. Strips ONLY leading exact 0.0f samples from the start of the piece (keeps all non-zero low-volume speech).
     * 2. Applies a 20ms raised-cosine fade-in (0 -> 1) at the start and a 20ms raised-cosine fade-out (1 -> 0)
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

        val result = samples.copyOfRange(firstNonZero, samples.size)
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

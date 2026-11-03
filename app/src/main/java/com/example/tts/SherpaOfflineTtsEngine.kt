package com.example.tts

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class ModelFileStatus(
    val isModelReady: Boolean,
    val isTokensReady: Boolean,
    val modelActualBytes: Long,
    val tokensActualBytes: Long,
    val isEngineLoadedInMemory: Boolean
) {
    val isFullyReadyOffline: Boolean
        get() = isModelReady && isTokensReady
}

data class SynthesisSuccess(
    val outputFile: File,
    val fileName: String,
    val durationSeconds: Float,
    val wordCount: Int,
    val chunkCount: Int,
    val fileSizeBytes: Long,
    val waveformPeaks: String
)

class BengaliTtsException(override val message: String, cause: Throwable? = null) :
    Exception(message, cause)

class SherpaOfflineTtsEngine(private val appContext: Context) {

    companion object {
        // Exact FIXED retrain model URL & byte size (v1.9.4-fixed)
        const val MODEL_URL =
            "https://huggingface.co/demoqqq/banglatts-eleven-new-retrain/resolve/main/model_fullmeta.onnx"
        const val MODEL_FILENAME = "model_fullmeta.onnx"
        const val EXPECTED_MODEL_BYTES = 114188118L

        // Exact Sherpa tokens URL & byte size
        const val TOKENS_URL =
            "https://huggingface.co/demoqqq/banglatts-eleven-new-retrain/resolve/main/tokens_sherpa.txt"
        const val TOKENS_FILENAME = "tokens_sherpa.txt"
        const val EXPECTED_TOKENS_BYTES = 567L

        // Exact VITS settings that produced the approved samples
        const val VITS_NOISE_SCALE = 0.667f
        const val VITS_NOISE_SCALE_W = 0.8f
        const val VITS_LENGTH_SCALE = 1.0f
        const val VITS_SAMPLE_RATE = 22050
        const val CPU_THREADS = 2

        private const val MAX_DOWNLOAD_RETRIES = 5
    }

    private val engineMutex = Mutex()
    private var offlineTts: OfflineTts? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun getModelFile(): File = File(appContext.filesDir, MODEL_FILENAME)
    fun getTokensFile(): File = File(appContext.filesDir, TOKENS_FILENAME)

    fun queryModelStatus(): ModelFileStatus {
        // Clean up any legacy broken model.onnx if present from older versions
        val oldBrokenModel = File(appContext.filesDir, "model.onnx")
        if (oldBrokenModel.exists()) {
            runCatching { oldBrokenModel.delete() }
        }

        val modelFile = getModelFile()
        val tokensFile = getTokensFile()
        val modelBytes = if (modelFile.exists()) modelFile.length() else 0L
        val tokensBytes = if (tokensFile.exists()) tokensFile.length() else 0L
        return ModelFileStatus(
            isModelReady = modelBytes == EXPECTED_MODEL_BYTES,
            isTokensReady = tokensBytes == EXPECTED_TOKENS_BYTES,
            modelActualBytes = modelBytes,
            tokensActualBytes = tokensBytes,
            isEngineLoadedInMemory = offlineTts != null
        )
    }

    /**
     * Ensures tokens_sherpa.txt (567 bytes) and model_fullmeta.onnx (114,188,118 bytes)
     * are downloaded into filesDir with HTTP Range resume and retry on failure.
     */
    suspend fun ensureModelFilesDownloaded(
        onProgress: (percent: Int, statusText: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        engineMutex.withLock {
            ensureFilesUnlocked(onProgress)
        }
    }

    private suspend fun ensureFilesUnlocked(
        onProgress: (percent: Int, statusText: String) -> Unit
    ) {
        val tokensFile = getTokensFile()
        if (!tokensFile.exists() || tokensFile.length() != EXPECTED_TOKENS_BYTES) {
            onProgress(1, "⏳ টোকেন ফাইল ডাউনলোড হচ্ছে (tokens_sherpa.txt - 567 bytes)...")
            downloadWithRangeResume(
                url = TOKENS_URL,
                targetFile = tokensFile,
                expectedBytes = EXPECTED_TOKENS_BYTES,
                label = "tokens_sherpa.txt"
            ) { _, _ -> }
        }

        val modelFile = getModelFile()
        if (!modelFile.exists() || modelFile.length() != EXPECTED_MODEL_BYTES) {
            onProgress(2, "⏳ প্রথমবার ভয়েস মডেল ডাউনলোড হচ্ছে (model_fullmeta.onnx - 108.9 MB)...")
            downloadWithRangeResume(
                url = MODEL_URL,
                targetFile = modelFile,
                expectedBytes = EXPECTED_MODEL_BYTES,
                label = "model_fullmeta.onnx"
            ) { downloaded, total ->
                val pct = ((downloaded.toDouble() / total.toDouble()) * 100.0)
                    .roundToInt()
                    .coerceIn(1, 99)
                val mbDone = downloaded / (1024f * 1024f)
                val mbTotal = total / (1024f * 1024f)
                onProgress(
                    pct,
                    "⏳ প্রথমবার ভয়েস মডেল ডাউনলোড হচ্ছে: %.1f / %.1f MB (%d%%)".format(
                        java.util.Locale.US,
                        mbDone,
                        mbTotal,
                        pct
                    )
                )
            }
        }
    }

    private suspend fun downloadWithRangeResume(
        url: String,
        targetFile: File,
        expectedBytes: Long,
        label: String,
        onBytesProgress: (downloadedBytes: Long, expectedBytes: Long) -> Unit
    ) {
        targetFile.parentFile?.mkdirs()

        // If targetFile already has the exact expected size, we're done
        if (targetFile.exists() && targetFile.length() == expectedBytes) {
            onBytesProgress(expectedBytes, expectedBytes)
            return
        }
        if (targetFile.exists() && targetFile.length() != expectedBytes) {
            targetFile.delete()
        }

        val partFile = File(targetFile.parentFile, "${targetFile.name}.part")
        if (partFile.exists() && partFile.length() > expectedBytes) {
            partFile.delete()
        }

        var lastError: Throwable? = null

        for (attempt in 1..MAX_DOWNLOAD_RETRIES) {
            try {
                val existingBytes = if (partFile.exists()) partFile.length() else 0L
                if (existingBytes == expectedBytes) {
                    if (partFile.renameTo(targetFile) && targetFile.length() == expectedBytes) {
                        onBytesProgress(expectedBytes, expectedBytes)
                        return
                    }
                }

                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "BanglaTTS-Android/1.9.4-fixed")

                if (existingBytes in 1 until expectedBytes) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        // If 416 Range Not Satisfiable, reset part file and retry
                        if (response.code == 416) {
                            partFile.delete()
                        }
                        throw BengaliTtsException(
                            "ডাউনলোড ব্যর্থ হয়েছে ($label): HTTP ${response.code}। ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।"
                        )
                    }

                    val isPartialContent = (response.code == 206 && existingBytes > 0L)
                    val startOffset = if (isPartialContent) existingBytes else 0L

                    val body = response.body ?: throw BengaliTtsException(
                        "সার্ভার থেকে খালি রেসপন্স পাওয়া গেছে ($label)।"
                    )

                    RandomAccessFile(partFile, "rw").use { raf ->
                        if (!isPartialContent) {
                            raf.setLength(0L)
                        }
                        raf.seek(startOffset)

                        body.byteStream().use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var currentTotal = startOffset
                            var lastReportedPercent = -1
                            var bytesRead: Int

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                raf.write(buffer, 0, bytesRead)
                                currentTotal += bytesRead
                                val pct = ((currentTotal * 100L) / expectedBytes).toInt()
                                if (pct != lastReportedPercent) {
                                    lastReportedPercent = pct
                                    onBytesProgress(currentTotal, expectedBytes)
                                }
                            }
                        }
                    }
                }

                val finalLength = partFile.length()
                if (finalLength != expectedBytes) {
                    // If oversized, delete partFile so next retry starts clean
                    if (finalLength > expectedBytes) {
                        partFile.delete()
                    }
                    throw BengaliTtsException(
                        "ফাইলের সাইজ মেলেনি ($label): পাওয়া গেছে $finalLength বাইট, প্রয়োজন $expectedBytes বাইট।"
                    )
                }

                if (targetFile.exists()) {
                    targetFile.delete()
                }
                val renamed = partFile.renameTo(targetFile)
                if (!renamed) {
                    partFile.copyTo(targetFile, overwrite = true)
                    partFile.delete()
                }

                if (targetFile.length() != expectedBytes) {
                    throw BengaliTtsException(
                        "সংরক্ষিত ফাইলের সাইজ সঠিক নয় ($label)।"
                    )
                }

                onBytesProgress(expectedBytes, expectedBytes)
                return
            } catch (t: Throwable) {
                lastError = t
                if (attempt < MAX_DOWNLOAD_RETRIES) {
                    delay(1200L * attempt)
                }
            }
        }

        val reason = lastError?.localizedMessage ?: "অজানা নেটওয়ার্ক ত্রুটি"
        throw BengaliTtsException(
            "ভয়েস মডেল ডাউনলোড করা যায়নি ($label)। অনুগ্রহ করে ইন্টারনেট সংযোগ পরীক্ষা করে আবার Generate চাপুন।\nবিস্তারিত: $reason",
            lastError
        )
    }

    /**
     * Lazily initializes OfflineTts on the FIRST Generate call only.
     * Never called during app startup.
     */
    private fun getOrLoadTtsUnlocked(): OfflineTts {
        offlineTts?.let { return it }

        val modelFile = getModelFile()
        val tokensFile = getTokensFile()

        if (!modelFile.exists() || modelFile.length() != EXPECTED_MODEL_BYTES) {
            throw BengaliTtsException(
                "ভয়েস মডেল ফাইল (model_fullmeta.onnx) সম্পূর্ণ পাওয়া যায়নি। অনুগ্রহ করে আবার চেষ্টা করুন।"
            )
        }
        if (!tokensFile.exists() || tokensFile.length() != EXPECTED_TOKENS_BYTES) {
            throw BengaliTtsException(
                "টোকেন ফাইল (tokens_sherpa.txt) পাওয়া যায়নি। অনুগ্রহ করে আবার চেষ্টা করুন।"
            )
        }

        try {
            val vitsConfig = OfflineTtsVitsModelConfig(
                model = modelFile.absolutePath,
                lexicon = "",
                tokens = tokensFile.absolutePath,
                dataDir = "",
                dictDir = "",
                noiseScale = VITS_NOISE_SCALE,
                noiseScaleW = VITS_NOISE_SCALE_W,
                lengthScale = VITS_LENGTH_SCALE
            )

            val modelConfig = OfflineTtsModelConfig(
                vits = vitsConfig,
                numThreads = CPU_THREADS,
                debug = false,
                provider = "cpu"
            )

            val config = OfflineTtsConfig(
                model = modelConfig,
                ruleFsts = "",
                ruleFars = "",
                maxNumSentences = 1,
                silenceScale = 1.0f
            )

            // CRITICAL: assetManager = null loads from filesDir paths directly
            val engine = OfflineTts(assetManager = null, config = config)
            offlineTts = engine
            return engine
        } catch (t: Throwable) {
            throw BengaliTtsException(
                "অফলাইন ভয়েস ইঞ্জিন লোড করতে সমস্যা হয়েছে: ${t.localizedMessage ?: "Native TTS Error"}",
                t
            )
        }
    }

    /**
     * Performs full offline Bengali speech synthesis:
     * 1. Downloads model_fullmeta.onnx & tokens_sherpa.txt on first run only if not yet present.
     * 2. Lazily loads Sherpa-ONNX OfflineTts VITS engine.
     * 3. Splits text using mandatory BengaliTextChunker rules.
     * 4. Synthesizes each chunk and joins them with exact silence pauses (0.40s, 0.30s, 0.20s).
     * 5. Saves 22050 Hz mono 16-bit WAV file in filesDir/generated_audio/.
     */
    suspend fun generateSpeech(
        scriptText: String,
        speed: Float,
        pitch: Float,
        preferredFilePrefix: String = "News_Script",
        onProgress: (percent: Int, statusText: String) -> Unit
    ): SynthesisSuccess = withContext(Dispatchers.IO) {
        engineMutex.withLock {
            val trimmed = scriptText.trim()
            if (trimmed.isEmpty()) {
                throw BengaliTtsException("দয়া করে ভয়েস জেনারেট করার জন্য বাংলা টেক্সট বা স্ক্রিপ্ট লিখুন।")
            }

            val wordCount = BengaliTextChunker.countWords(trimmed)
            if (wordCount > BengaliTextChunker.MAX_WORD_LIMIT) {
                throw BengaliTtsException(
                    "দয়া করে ৫০০০ শব্দের কম টেক্সট একসাথে ইনপুট দিন, যাতে অফলাইন প্রসেসিংয়ে কোনো ক্র্যাশ না করে।"
                )
            }

            val chunks = BengaliTextChunker.chunkText(trimmed)
            if (chunks.isEmpty()) {
                throw BengaliTtsException("টেক্সট প্রসেস করা যায়নি। অনুগ্রহ করে বৈধ বাংলা বাক্য লিখুন।")
            }

            // Step 1: Ensure model_fullmeta.onnx and tokens_sherpa.txt exist in filesDir
            val currentStatus = queryModelStatus()
            val needsDownload = !currentStatus.isFullyReadyOffline
            if (needsDownload) {
                ensureFilesUnlocked { dlPct, dlMessage ->
                    val scaledPct = (dlPct * 0.45f).roundToInt().coerceIn(1, 45)
                    onProgress(scaledPct, dlMessage)
                }
            }

            val basePct = if (needsDownload) 45 else 5
            onProgress(basePct, "⏳ অফলাইন ভয়েস ইঞ্জিন প্রস্তুত করা হচ্ছে (Sherpa-ONNX VITS)...")

            // Step 2: Lazily load OfflineTts
            val tts = getOrLoadTtsUnlocked()
            val sampleRate = runCatching { tts.sampleRate() }
                .getOrDefault(VITS_SAMPLE_RATE)
                .let { if (it > 0) it else VITS_SAMPLE_RATE }

            // Step 3: Synthesize chunks and join with silence pauses
            val chunkBuffers = ArrayList<FloatArray>(chunks.size * 2)
            var totalSamples = 0
            val safeSpeed = speed.coerceIn(0.5f, 2.0f)

            for ((index, chunk) in chunks.withIndex()) {
                val progressFraction = (index.toFloat() / max(1, chunks.size).toFloat())
                val currentPct = (basePct + ((92 - basePct) * progressFraction)).roundToInt()
                    .coerceIn(basePct, 92)
                onProgress(
                    currentPct,
                    "⏳ Generating offline audio... (অংশ ${index + 1}/${chunks.size})"
                )

                val generated = try {
                    tts.generate(
                        text = chunk.text,
                        sid = 0,
                        speed = safeSpeed
                    )
                } catch (t: Throwable) {
                    throw BengaliTtsException(
                        "অংশ ${index + 1} জেনারেট করতে সমস্যা হয়েছে: ${t.localizedMessage ?: "Synthesis error"}",
                        t
                    )
                }

                val rawSamples = generated.samples
                if (rawSamples.isNotEmpty()) {
                    val processedSamples = WavFileWriter.applyPitchIfNeeded(rawSamples, pitch)
                    chunkBuffers.add(processedSamples)
                    totalSamples += processedSamples.size
                }

                // Append silence pause between pieces (or after the piece's punctuation mark)
                if (index < chunks.lastIndex && chunk.pauseSeconds > 0f) {
                    val silenceCount = chunk.silenceSampleCount(sampleRate)
                    if (silenceCount > 0) {
                        val silence = FloatArray(silenceCount) // 0.0f silence
                        chunkBuffers.add(silence)
                        totalSamples += silence.size
                    }
                }
            }

            if (totalSamples == 0) {
                throw BengaliTtsException(
                    "এই টেক্সট থেকে কোনো অডিও স্যাম্পল তৈরি হয়নি। অনুগ্রহ করে বাংলা ইউনিকোড টেক্সট দিন।"
                )
            }

            onProgress(95, "⏳ ১৬-বিট মনো WAV অডিও ফাইল সংরক্ষণ করা হচ্ছে...")

            val combinedSamples = FloatArray(totalSamples)
            var offset = 0
            for (buf in chunkBuffers) {
                System.arraycopy(buf, 0, combinedSamples, offset, buf.size)
                offset += buf.size
            }

            val audioDir = File(appContext.filesDir, "generated_audio").apply { mkdirs() }
            val timestampSuffix = (System.currentTimeMillis() % 100000)
            val sanitizedPrefix = preferredFilePrefix
                .replace(Regex("[^A-Za-z0-9_]"), "_")
                .trim('_')
                .ifEmpty { "News_Script" }
            val fileName = "${sanitizedPrefix}_${timestampSuffix}.wav"
            val outputFile = File(audioDir, fileName)

            WavFileWriter.writeMono16BitWav(
                samples = combinedSamples,
                sampleRate = sampleRate,
                outputFile = outputFile
            )

            val waveformPeaks = WavFileWriter.computeWaveformPeaks(combinedSamples)
            val durationSeconds = totalSamples.toFloat() / sampleRate.toFloat()

            onProgress(100, "✅ অডিও জেনারেশন সম্পন্ন হয়েছে!")

            SynthesisSuccess(
                outputFile = outputFile,
                fileName = fileName,
                durationSeconds = durationSeconds,
                wordCount = wordCount,
                chunkCount = chunks.size,
                fileSizeBytes = outputFile.length(),
                waveformPeaks = waveformPeaks
            )
        }
    }

    fun release() {
        runCatching {
            offlineTts?.release()
            offlineTts = null
        }
    }
}

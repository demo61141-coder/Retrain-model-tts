package com.example.ui

import android.app.Application
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TtsDatabase
import com.example.data.TtsHistoryEntity
import com.example.data.TtsRepository
import com.example.tts.AudioExportHelper
import com.example.tts.BengaliTextChunker
import com.example.tts.ModelFileStatus
import com.example.tts.NewsTemplate
import com.example.tts.NewsTemplates
import com.example.tts.SherpaOfflineTtsEngine
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class StudioDestination {
    HOME,
    HISTORY,
    TTS_SETUP,
    AUDIO_PLAYER
}

data class NoticeDialogState(
    val title: String = "⚠️ Notice",
    val message: String,
    val isWordLimitWarning: Boolean = false
)

data class StudioUiState(
    val currentDestination: StudioDestination = StudioDestination.HOME,
    val previousDestination: StudioDestination = StudioDestination.HOME,
    val selectedTemplate: NewsTemplate = NewsTemplates.templates.first(),
    val scriptText: String = NewsTemplates.templates.first().sampleScript,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val isGenerating: Boolean = false,
    val progressPercent: Int = 0,
    val progressStatusText: String = "",
    val modelStatus: ModelFileStatus = ModelFileStatus(
        isModelReady = false,
        isTokensReady = false,
        isBundledInApkAssets = true,
        modelActualBytes = SherpaOfflineTtsEngine.EXPECTED_MODEL_BYTES,
        tokensActualBytes = SherpaOfflineTtsEngine.EXPECTED_TOKENS_BYTES,
        isEngineLoadedInMemory = false
    ),
    val activeAudioItem: TtsHistoryEntity? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Int = 0,
    val playbackDurationMs: Int = 0,
    val noticeDialog: NoticeDialogState? = null,
    val toastBannerMessage: String? = null,
    val historySearchQuery: String = ""
) {
    val wordCount: Int
        get() = BengaliTextChunker.countWords(scriptText)

    val previewChunksCount: Int
        get() = BengaliTextChunker.chunkText(scriptText).size

    val pitchLabel: String
        get() = when {
            pitch < 0.93f -> "Deep (${"%.2f".format(java.util.Locale.US, pitch)}x)"
            pitch > 1.07f -> "High (${"%.2f".format(java.util.Locale.US, pitch)}x)"
            else -> "Normal (1.0x)"
        }
}

class TtsStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = TtsDatabase.getInstance(appContext)
    private val repository = TtsRepository(database.ttsHistoryDao())
    private val ttsEngine = SherpaOfflineTtsEngine.getInstance(appContext)

    private val _uiState = MutableStateFlow(
        StudioUiState(
            modelStatus = ttsEngine.queryModelStatus()
        )
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val historyList: StateFlow<List<TtsHistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var mediaPlayer: MediaPlayer? = null
    private var playbackTickerJob: Job? = null
    private var systemTtsTester: TextToSpeech? = null

    fun refreshModelStatus() {
        _uiState.update { it.copy(modelStatus = ttsEngine.queryModelStatus()) }
    }

    fun navigateTo(destination: StudioDestination) {
        _uiState.update { state ->
            val prev = if (state.currentDestination != StudioDestination.AUDIO_PLAYER) {
                state.currentDestination
            } else {
                state.previousDestination
            }
            state.copy(
                currentDestination = destination,
                previousDestination = prev
            )
        }
        refreshModelStatus()
    }

    fun navigateBack() {
        _uiState.update { state ->
            val target = when (state.currentDestination) {
                StudioDestination.AUDIO_PLAYER -> state.previousDestination
                StudioDestination.HISTORY, StudioDestination.TTS_SETUP -> StudioDestination.HOME
                StudioDestination.HOME -> StudioDestination.HOME
            }
            state.copy(currentDestination = target)
        }
    }

    fun selectTemplate(template: NewsTemplate) {
        _uiState.update { state ->
            state.copy(
                selectedTemplate = template,
                scriptText = if (template.id == "custom_script") "" else template.sampleScript
            )
        }
    }

    fun updateScriptText(newText: String) {
        val words = BengaliTextChunker.countWords(newText)
        if (words > BengaliTextChunker.MAX_WORD_LIMIT) {
            _uiState.update {
                it.copy(
                    scriptText = newText,
                    noticeDialog = NoticeDialogState(
                        title = "⚠️ Notice",
                        message = "দয়া করে ৫০০০ শব্দের কম টেক্সট একসাথে ইনপুট দিন, যাতে অফলাইন প্রসেসিংয়ে কোনো ক্র্যাশ না করে।",
                        isWordLimitWarning = true
                    )
                )
            }
        } else {
            _uiState.update { it.copy(scriptText = newText) }
        }
    }

    fun updateSpeed(newSpeed: Float) {
        val rounded = ((newSpeed * 20f).toInt() / 20f).coerceIn(0.5f, 1.5f)
        _uiState.update { it.copy(speed = rounded) }
    }

    fun updatePitch(newPitch: Float) {
        val rounded = ((newPitch * 20f).toInt() / 20f).coerceIn(0.8f, 1.2f)
        _uiState.update { it.copy(pitch = rounded) }
    }

    fun resetVoiceSettings() {
        _uiState.update { it.copy(speed = 1.0f, pitch = 1.0f) }
    }

    fun updateHistorySearchQuery(query: String) {
        _uiState.update { it.copy(historySearchQuery = query) }
    }

    fun dismissNoticeDialog() {
        _uiState.update { it.copy(noticeDialog = null) }
    }

    fun clearToastBanner() {
        _uiState.update { it.copy(toastBannerMessage = null) }
    }

    private fun showBanner(message: String) {
        _uiState.update { it.copy(toastBannerMessage = message) }
    }

    /**
     * Triggered when user presses [ GENERATE VOICE 🔊 ].
     * Uses the built-in offline model_fullmeta.onnx + tokens_sherpa.txt from APK assets (no internet download needed),
     * chunks text per exact rules, synthesizes WAV, saves record to Room local DB, and opens Audio Player.
     */
    fun generateVoice() {
        val currentState = _uiState.value
        if (currentState.isGenerating) return

        val trimmed = currentState.scriptText.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(
                    noticeDialog = NoticeDialogState(
                        title = "⚠️ Notice",
                        message = "ভয়েস জেনারেট করার আগে অনুগ্রহ করে বক্সে আপনার বাংলা নিউজ বা স্ক্রিপ্ট লিখুন বা পেস্ট করুন।"
                    )
                )
            }
            return
        }

        val wordCount = BengaliTextChunker.countWords(trimmed)
        if (wordCount > BengaliTextChunker.MAX_WORD_LIMIT) {
            _uiState.update {
                it.copy(
                    noticeDialog = NoticeDialogState(
                        title = "⚠️ Notice",
                        message = "দয়া করে ৫০০০ শব্দের কম টেক্সট একসাথে ইনপুট দিন, যাতে অফলাইন প্রসেসিংয়ে কোনো ক্র্যাশ না করে।",
                        isWordLimitWarning = true
                    )
                )
            }
            return
        }

        stopPlayback()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    progressPercent = 1,
                    progressStatusText = "⏳ Generating offline audio... (Please wait)"
                )
            }

            try {
                val result = ttsEngine.generateSpeech(
                    scriptText = trimmed,
                    speed = currentState.speed,
                    pitch = currentState.pitch,
                    preferredFilePrefix = currentState.selectedTemplate.filePrefix
                ) { pct, statusMessage ->
                    _uiState.update { state ->
                        state.copy(
                            progressPercent = pct.coerceIn(0, 100),
                            progressStatusText = statusMessage
                        )
                    }
                }

                val sha256 = TtsRepository.computeSha256(result.outputFile)
                val entity = TtsHistoryEntity(
                    fileName = result.fileName,
                    filePath = result.outputFile.absolutePath,
                    scriptText = trimmed,
                    templateName = currentState.selectedTemplate.title,
                    durationSeconds = result.durationSeconds,
                    wordCount = result.wordCount,
                    chunkCount = result.chunkCount,
                    speed = currentState.speed,
                    pitch = currentState.pitch,
                    fileSizeBytes = result.fileSizeBytes,
                    waveformPeaks = result.waveformPeaks,
                    sha256Checksum = sha256
                )

                val insertedId = repository.insert(entity)
                val savedEntity = entity.copy(id = insertedId)

                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        progressPercent = 100,
                        progressStatusText = "",
                        modelStatus = ttsEngine.queryModelStatus(),
                        activeAudioItem = savedEntity,
                        previousDestination = StudioDestination.HOME,
                        currentDestination = StudioDestination.AUDIO_PLAYER,
                        toastBannerMessage = "✅ অডিও তৈরি সম্পন্ন হয়েছে: ${savedEntity.fileName}"
                    )
                }

                startPlayback(savedEntity, autoPlay = true)
            } catch (t: Throwable) {
                val errorText = t.message?.takeIf { it.isNotBlank() }
                    ?: "অফলাইন অডিও তৈরিতে ত্রুটি হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        progressPercent = 0,
                        progressStatusText = "",
                        modelStatus = ttsEngine.queryModelStatus(),
                        noticeDialog = NoticeDialogState(
                            title = "⚠️ Notice",
                            message = errorText
                        )
                    )
                }
            }
        }
    }

    /**
     * Prepares/extracts the built-in offline model into filesDir immediately.
     */
    fun prepareOfflineModelFiles() {
        val currentState = _uiState.value
        if (currentState.isGenerating) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    progressPercent = 1,
                    progressStatusText = "⏳ বিল্ট-ইন অফলাইন ভয়েস মডেল প্রস্তুত হচ্ছে..."
                )
            }
            try {
                ttsEngine.ensureModelFilesReady { pct, statusMessage ->
                    _uiState.update { state ->
                        state.copy(
                            progressPercent = pct.coerceIn(0, 100),
                            progressStatusText = statusMessage
                        )
                    }
                }
                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        progressPercent = 100,
                        progressStatusText = "",
                        modelStatus = ttsEngine.queryModelStatus(),
                        toastBannerMessage = "✅ বিল্ট-ইন অফলাইন ভয়েস মডেল (model_fullmeta.onnx) সম্পূর্ণ প্রস্তুত!"
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        progressPercent = 0,
                        progressStatusText = "",
                        modelStatus = ttsEngine.queryModelStatus(),
                        noticeDialog = NoticeDialogState(
                            title = "⚠️ মডেল ত্রুটি",
                            message = t.message ?: "অফলাইন মডেল প্রস্তুত করতে সমস্যা হয়েছে।"
                        )
                    )
                }
            }
        }
    }

    /**
     * Tests the Android System-wide TextToSpeechService (ReporterHasanTtsService)
     * directly via Android's TextToSpeech API so the user can verify phone TTS integration.
     */
    fun testSystemTtsEngine() {
        showBanner("🔊 সিস্টেম TTS ইঞ্জিন (Reporter Hasan TTS) টেস্ট করা হচ্ছে...")
        runCatching {
            systemTtsTester?.shutdown()
            systemTtsTester = TextToSpeech(
                appContext,
                { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        systemTtsTester?.language = Locale("bn", "BD")
                        systemTtsTester?.speak(
                            "রিপোর্টার হাসান অফলাইন সিস্টেম টিটিএস ইঞ্জিন এখন আপনার ফোনের সাথে সংযুক্ত।",
                            TextToSpeech.QUEUE_FLUSH,
                            Bundle(),
                            "system_tts_test_utterance"
                        )
                    } else {
                        showBanner("⚠️ সিস্টেম TTS ইনিশিয়ালাইজ করা যায়নি।")
                    }
                },
                appContext.packageName
            )
        }
    }

    fun openAudioPlayerForItem(item: TtsHistoryEntity, autoPlay: Boolean = true) {
        val file = File(item.filePath)
        if (!file.exists()) {
            _uiState.update {
                it.copy(
                    noticeDialog = NoticeDialogState(
                        title = "⚠️ ফাইল পাওয়া যায়নি",
                        message = "এই অডিও ফাইলটি ডিভাইস থেকে মুছে গেছে। অনুগ্রহ করে আবার জেনারেট করুন।"
                    )
                )
            }
            return
        }

        _uiState.update { state ->
            val prev = if (state.currentDestination != StudioDestination.AUDIO_PLAYER) {
                state.currentDestination
            } else {
                state.previousDestination
            }
            state.copy(
                activeAudioItem = item,
                previousDestination = prev,
                currentDestination = StudioDestination.AUDIO_PLAYER
            )
        }
        startPlayback(item, autoPlay = autoPlay)
    }

    private fun startPlayback(item: TtsHistoryEntity, autoPlay: Boolean) {
        stopPlayback()
        val file = File(item.filePath)
        if (!file.exists()) return

        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener { mp ->
                    playbackTickerJob?.cancel()
                    _uiState.update { state ->
                        state.copy(
                            isPlaying = false,
                            playbackPositionMs = mp.duration.coerceAtLeast(0)
                        )
                    }
                }
            }
            mediaPlayer = player
            val durationMs = max(player.duration, (item.durationSeconds * 1000).toInt())
            _uiState.update {
                it.copy(
                    playbackPositionMs = 0,
                    playbackDurationMs = durationMs,
                    isPlaying = autoPlay
                )
            }
            if (autoPlay) {
                player.start()
                startPlaybackTicker()
            }
        } catch (t: Throwable) {
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    noticeDialog = NoticeDialogState(
                        title = "⚠️ প্লেব্যাক ত্রুটি",
                        message = "অডিও ফাইলটি প্লে করা যায়নি: ${t.localizedMessage ?: "Audio error"}"
                    )
                )
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        val activeItem = _uiState.value.activeAudioItem
        if (player == null) {
            if (activeItem != null) {
                startPlayback(activeItem, autoPlay = true)
            }
            return
        }

        try {
            if (player.isPlaying) {
                player.pause()
                playbackTickerJob?.cancel()
                _uiState.update {
                    it.copy(
                        isPlaying = false,
                        playbackPositionMs = player.currentPosition
                    )
                }
            } else {
                if (player.currentPosition >= player.duration - 150) {
                    player.seekTo(0)
                }
                player.start()
                _uiState.update { it.copy(isPlaying = true) }
                startPlaybackTicker()
            }
        } catch (_: Throwable) {
            if (activeItem != null) {
                startPlayback(activeItem, autoPlay = true)
            }
        }
    }

    fun seekToFraction(fraction: Float) {
        val player = mediaPlayer ?: return
        runCatching {
            val duration = max(1, _uiState.value.playbackDurationMs)
            val targetMs = (duration * fraction.coerceIn(0f, 1f)).toInt()
            player.seekTo(targetMs)
            _uiState.update { it.copy(playbackPositionMs = targetMs) }
        }
    }

    fun seekRelativeMs(deltaMs: Int) {
        val player = mediaPlayer ?: return
        runCatching {
            val duration = max(1, _uiState.value.playbackDurationMs)
            val targetMs = (player.currentPosition + deltaMs).coerceIn(0, duration)
            player.seekTo(targetMs)
            _uiState.update { it.copy(playbackPositionMs = targetMs) }
        }
    }

    private fun startPlaybackTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = viewModelScope.launch {
            while (isActive) {
                val player = mediaPlayer ?: break
                val playing = runCatching { player.isPlaying }.getOrDefault(false)
                if (!playing) break
                val pos = runCatching { player.currentPosition }.getOrDefault(0)
                val dur = runCatching { player.duration }.getOrDefault(_uiState.value.playbackDurationMs)
                _uiState.update {
                    it.copy(
                        isPlaying = true,
                        playbackPositionMs = min(pos, max(pos, dur)),
                        playbackDurationMs = max(dur, it.playbackDurationMs)
                    )
                }
                delay(100L)
            }
        }
    }

    fun stopPlayback() {
        playbackTickerJob?.cancel()
        playbackTickerJob = null
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _uiState.update {
            it.copy(
                isPlaying = false,
                playbackPositionMs = 0
            )
        }
    }

    fun saveAudioToDevice(item: TtsHistoryEntity) {
        viewModelScope.launch {
            val source = File(item.filePath)
            val result = AudioExportHelper.saveWavToDevice(
                context = appContext,
                sourceFile = source,
                displayFileName = item.fileName
            )
            result.onSuccess { (_, message) ->
                showBanner("📥 $message")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        noticeDialog = NoticeDialogState(
                            title = "⚠️ সংরক্ষণ ব্যর্থ",
                            message = err.localizedMessage ?: "ডিভাইসে ফাইল সেভ করা যায়নি।"
                        )
                    )
                }
            }
        }
    }

    fun shareAudioItem(item: TtsHistoryEntity) {
        viewModelScope.launch {
            val source = File(item.filePath)
            val result = AudioExportHelper.shareWavAudio(
                context = appContext,
                sourceFile = source,
                displayFileName = item.fileName,
                scriptSnippet = item.scriptText
            )
            result.onFailure { err ->
                _uiState.update {
                    it.copy(
                        noticeDialog = NoticeDialogState(
                            title = "⚠️ শেয়ার ব্যর্থ",
                            message = err.localizedMessage ?: "অডিও ফাইল শেয়ার করা যায়নি।"
                        )
                    )
                }
            }
        }
    }

    fun deleteHistoryItem(item: TtsHistoryEntity) {
        viewModelScope.launch {
            if (_uiState.value.activeAudioItem?.id == item.id) {
                stopPlayback()
                _uiState.update { it.copy(activeAudioItem = null) }
            }
            repository.deleteItem(item)
            showBanner("🗑️ মুছে ফেলা হয়েছে: ${item.fileName}")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            stopPlayback()
            val items = historyList.value
            repository.clearAllHistory(items)
            _uiState.update { it.copy(activeAudioItem = null) }
            showBanner("🗑️ সকল অডিও হিস্ট্রি মুছে ফেলা হয়েছে।")
        }
    }

    fun loadHistoryScriptIntoEditor(item: TtsHistoryEntity) {
        _uiState.update {
            it.copy(
                scriptText = item.scriptText,
                speed = item.speed,
                pitch = item.pitch,
                currentDestination = StudioDestination.HOME,
                toastBannerMessage = "📝 স্ক্রিপ্টটি এডিটরে লোড করা হয়েছে।"
            )
        }
    }

    fun openSystemTtsSettings() {
        AudioExportHelper.openPhoneTtsSettings(appContext).onFailure {
            showBanner("ফোনের সেটিংস খোলা যায়নি।")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        runCatching { systemTtsTester?.shutdown() }
    }
}

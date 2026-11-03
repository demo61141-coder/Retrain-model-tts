package com.example.tts

import android.app.Activity
import android.content.Intent
import android.media.AudioFormat
import android.os.Bundle
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import com.example.MainActivity
import java.util.Locale
import kotlin.math.min
import kotlinx.coroutines.runBlocking

/**
 * Android System-wide TextToSpeechService so the user can select "Reporter Hasan TTS"
 * in Phone Settings -> Text-to-speech output, and any other speech/reading app on the phone
 * will speak using Reporter Hasan's offline Bengali voice!
 */
class ReporterHasanTtsService : TextToSpeechService() {

    @Volatile
    private var isStopped: Boolean = false

    private val currentLanguage = arrayOf("ben", "BGD", "reporter_hasan")

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val normalized = lang?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when {
            normalized.isEmpty() -> TextToSpeech.LANG_AVAILABLE
            normalized == "bn" || normalized == "ben" || normalized.startsWith("ben") || normalized.startsWith("bn") -> {
                if (!country.isNullOrBlank()) {
                    TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
                } else {
                    TextToSpeech.LANG_AVAILABLE
                }
            }
            // Also accept English or default requests so third-party TTS apps that default to en-US
            // can still synthesize text through Reporter Hasan TTS without failing language checks.
            normalized == "en" || normalized == "eng" || normalized.startsWith("en") -> {
                TextToSpeech.LANG_AVAILABLE
            }
            else -> TextToSpeech.LANG_AVAILABLE
        }
    }

    override fun onGetLanguage(): Array<String> {
        return currentLanguage
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        if (!lang.isNullOrBlank()) {
            currentLanguage[0] = lang
            currentLanguage[1] = country.orEmpty().ifEmpty { "BGD" }
            currentLanguage[2] = variant.orEmpty().ifEmpty { "reporter_hasan" }
        }
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onGetFeaturesForLanguage(
        lang: String?,
        country: String?,
        variant: String?
    ): MutableSet<String> {
        // Empty set = 100% offline engine (does not require network synthesis)
        return mutableSetOf()
    }

    override fun onGetVoices(): MutableList<Voice> {
        val voices = mutableListOf<Voice>()
        runCatching {
            val bdLocale = Locale("bn", "BD")
            val inLocale = Locale("bn", "IN")
            val usLocale = Locale("en", "US")
            voices.add(
                Voice(
                    "bn-bd-reporter-hasan-offline",
                    bdLocale,
                    Voice.QUALITY_VERY_HIGH,
                    Voice.LATENCY_LOW,
                    false,
                    emptySet()
                )
            )
            voices.add(
                Voice(
                    "bn-in-reporter-hasan-offline",
                    inLocale,
                    Voice.QUALITY_VERY_HIGH,
                    Voice.LATENCY_LOW,
                    false,
                    emptySet()
                )
            )
            voices.add(
                Voice(
                    "en-us-reporter-hasan-offline",
                    usLocale,
                    Voice.QUALITY_HIGH,
                    Voice.LATENCY_LOW,
                    false,
                    emptySet()
                )
            )
        }
        return voices
    }

    override fun onIsValidVoiceName(voiceName: String?): Int {
        return TextToSpeech.SUCCESS
    }

    override fun onLoadVoice(voiceName: String?): Int {
        return TextToSpeech.SUCCESS
    }

    override fun onGetDefaultVoiceNameFor(
        lang: String?,
        country: String?,
        variant: String?
    ): String {
        return "bn-bd-reporter-hasan-offline"
    }

    override fun onStop() {
        isStopped = true
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return
        isStopped = false

        val text = runCatching {
            request.charSequenceText?.toString()
        }.getOrNull() ?: request.text.orEmpty()

        val trimmed = text.trim()
        // Start 22050 Hz, 16-bit PCM, Mono stream
        callback.start(
            SherpaOfflineTtsEngine.VITS_SAMPLE_RATE,
            AudioFormat.ENCODING_PCM_16BIT,
            1
        )

        if (trimmed.isEmpty()) {
            callback.done()
            return
        }

        // Convert Android speechRate (100 = 1.0x) and pitch (100 = 1.0x)
        val rateVal = request.speechRate.takeIf { it > 0 } ?: 100
        val pitchVal = request.pitch.takeIf { it > 0 } ?: 100
        val speed = (rateVal / 100.0f).coerceIn(0.5f, 2.0f)
        val pitch = (pitchVal / 100.0f).coerceIn(0.8f, 1.2f)

        val engine = SherpaOfflineTtsEngine.getInstance(applicationContext)
        val maxBufferSize = callback.maxBufferSize.coerceAtLeast(1024)

        try {
            runBlocking {
                engine.synthesizeForSystemTtsService(
                    rawText = trimmed,
                    speed = speed,
                    pitch = pitch,
                    isStopped = { isStopped }
                ) { pcmBytes ->
                    var offset = 0
                    while (offset < pcmBytes.size && !isStopped) {
                        val bytesToWrite = min(maxBufferSize, pcmBytes.size - offset)
                        val rc = callback.audioAvailable(pcmBytes, offset, bytesToWrite)
                        if (rc == TextToSpeech.ERROR) {
                            return@synthesizeForSystemTtsService false
                        }
                        offset += bytesToWrite
                    }
                    !isStopped
                }
            }
            if (!isStopped) {
                callback.done()
            }
        } catch (_: Throwable) {
            runCatching { callback.error() }
        }
    }
}

/**
 * Companion activity invoked by Android's Text-to-Speech Settings when verifying
 * whether Reporter Hasan TTS has its offline voice files installed.
 */
class CheckVoiceDataActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val availableVoices = arrayListOf(
            "ben-BGD",
            "ben-IND",
            "ben",
            "bn-BD",
            "bn-IN",
            "bn",
            "eng-USA",
            "eng-GBR",
            "eng"
        )
        val resultIntent = Intent().apply {
            putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, availableVoices)
            putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, arrayListOf())
        }
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, resultIntent)
        finish()
    }
}

/**
 * Companion activity invoked by Android's Text-to-Speech Settings when the user taps
 * "Listen to an example / Play sample" in Phone TTS Settings.
 */
class GetSampleTextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sampleText =
            "শুভ সন্ধ্যা, রিপোর্টার হাসান নিউজ ডেস্কে আপনাকে স্বাগতম। সম্পূর্ণ অফলাইন বাংলা ভয়েস ইঞ্জিন এখন আপনার ফোনে সচল রয়েছে।"
        val resultIntent = Intent().apply {
            putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, sampleText)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}

/**
 * Companion activity for Android's INSTALL_TTS_DATA intent.
 */
class InstallVoiceDataActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

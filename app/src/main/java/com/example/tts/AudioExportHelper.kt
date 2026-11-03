package com.example.tts

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioExportHelper {

    /**
     * Saves the generated WAV file into public Downloads/ReporterHasanTTS (API 29+)
     * or Music/ReporterHasanTTS so the user can access it in their device file manager.
     * Returns the MediaStore Uri and human-readable location message.
     */
    suspend fun saveWavToDevice(
        context: Context,
        sourceFile: File,
        displayFileName: String
    ): Result<Pair<Uri?, String>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                throw IllegalStateException("অডিও ফাইলটি খুঁজে পাওয়া যায়নি।")
            }

            val cleanName = if (displayFileName.endsWith(".wav", ignoreCase = true)) {
                displayFileName
            } else {
                "$displayFileName.wav"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/ReporterHasanTTS"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("ডিভাইস স্টোরেজে ফাইল তৈরি করা যায়নি।")

                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().buffered().use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("outputStream খোলা যায়নি।")

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                uri to "Downloads/ReporterHasanTTS/$cleanName ফোল্ডারে সংরক্ষিত হয়েছে!"
            } else {
                val exportDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                    "ReporterHasanTTS"
                ).apply { mkdirs() }
                val destFile = File(exportDir, cleanName)
                sourceFile.copyTo(destFile, overwrite = true)
                null to "${destFile.absolutePath}-এ সংরক্ষিত হয়েছে!"
            }
        }
    }

    /**
     * Publishes the WAV to MediaStore Audio (if on API 29+) so it gets a system content:// URI
     * that can be shared with external apps without needing a custom ContentProvider in AndroidManifest.xml.
     */
    suspend fun shareWavAudio(
        context: Context,
        sourceFile: File,
        displayFileName: String,
        scriptSnippet: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!sourceFile.exists()) {
                throw IllegalStateException("শেয়ার করার জন্য অডিও ফাইলটি পাওয়া যায়নি।")
            }

            val cleanName = if (displayFileName.endsWith(".wav", ignoreCase = true)) {
                displayFileName
            } else {
                "$displayFileName.wav"
            }

            var shareUri: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, cleanName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(
                        MediaStore.Audio.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_MUSIC + "/ReporterHasanTTS"
                    )
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val insertedUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                if (insertedUri != null) {
                    resolver.openOutputStream(insertedUri)?.use { out ->
                        sourceFile.inputStream().buffered().use { input ->
                            input.copyTo(out)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(insertedUri, values, null, null)
                    shareUri = insertedUri
                }
            }

            withContext(Dispatchers.Main) {
                val sendIntent = if (shareUri != null) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "audio/wav"
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "🎙️ Reporter Hasan TTS Generated Audio ($cleanName)\n\n\"${scriptSnippet.take(160)}\""
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "🎙️ Reporter Hasan TTS Script ($cleanName):\n\n$scriptSnippet"
                        )
                    }
                }

                val chooser = Intent.createChooser(sendIntent, "Share Audio ($cleanName)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        }
    }

    /**
     * Opens the phone's Text-to-Speech output settings screen safely.
     */
    fun openPhoneTtsSettings(context: Context): Result<Unit> {
        return runCatching {
            val ttsIntent = Intent("com.android.settings.TTS_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(ttsIntent)
            } catch (_: Throwable) {
                val fallbackIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(fallbackIntent)
                } catch (_: Throwable) {
                    val generalSettings = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(generalSettings)
                }
            }
        }
    }
}

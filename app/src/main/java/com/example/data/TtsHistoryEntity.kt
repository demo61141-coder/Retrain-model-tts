package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tts_history")
data class TtsHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val scriptText: String,
    val templateName: String,
    val durationSeconds: Float,
    val wordCount: Int,
    val chunkCount: Int,
    val speed: Float,
    val pitch: Float,
    val fileSizeBytes: Long,
    val waveformPeaks: String,
    val sha256Checksum: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun parsedWaveformPeaks(): List<Float> {
        if (waveformPeaks.isBlank()) return List(36) { 0.35f }
        return waveformPeaks.split(",")
            .mapNotNull { it.trim().toFloatOrNull()?.coerceIn(0.08f, 1.0f) }
            .ifEmpty { List(36) { 0.35f } }
    }
}

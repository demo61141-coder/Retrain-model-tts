package com.example.data

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TtsRepository(private val dao: TtsHistoryDao) {

    val allHistory: Flow<List<TtsHistoryEntity>> = dao.getAllHistory()

    suspend fun insert(entity: TtsHistoryEntity): Long = withContext(Dispatchers.IO) {
        dao.insertHistory(entity)
    }

    suspend fun deleteItem(entity: TtsHistoryEntity) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(entity.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        dao.deleteById(entity.id)
    }

    suspend fun clearAllHistory(items: List<TtsHistoryEntity>) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            runCatching {
                val file = File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
        dao.deleteAll()
    }

    suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        dao.getCount()
    }

    suspend fun verifyFileIntegrity(entity: TtsHistoryEntity): Boolean = withContext(Dispatchers.IO) {
        val file = File(entity.filePath)
        if (!file.exists() || file.length() == 0L) return@withContext false
        if (entity.sha256Checksum.isBlank()) return@withContext true
        val actualHash = computeSha256(file)
        actualHash.equals(entity.sha256Checksum, ignoreCase = true)
    }

    companion object {
        fun computeSha256(file: File): String {
            return runCatching {
                val digest = MessageDigest.getInstance("SHA-256")
                file.inputStream().buffered().use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        digest.update(buffer, 0, read)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }.getOrDefault("")
        }
    }
}

package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TtsHistoryDao {
    @Query("SELECT * FROM tts_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<TtsHistoryEntity>>

    @Query("SELECT COUNT(*) FROM tts_history")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: TtsHistoryEntity): Long

    @Query("DELETE FROM tts_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tts_history")
    suspend fun deleteAll()
}

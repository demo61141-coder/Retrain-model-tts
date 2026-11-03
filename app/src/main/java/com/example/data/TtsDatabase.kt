package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TtsHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TtsDatabase : RoomDatabase() {
    abstract fun ttsHistoryDao(): TtsHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TtsDatabase? = null

        fun getInstance(context: Context): TtsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TtsDatabase::class.java,
                    "reporter_hasan_tts_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

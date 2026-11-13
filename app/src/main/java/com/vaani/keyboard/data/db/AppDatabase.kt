package com.vaani.keyboard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VocabEntryEntity::class, SnippetEntity::class, ModelEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao
    abstract fun snippetDao(): SnippetDao
    abstract fun modelDao(): ModelDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "vaani.db")
                    .build()
                    .also { instance = it }
            }
    }
}

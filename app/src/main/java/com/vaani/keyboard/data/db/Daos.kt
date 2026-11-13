package com.vaani.keyboard.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {
    @Query("SELECT * FROM vocab ORDER BY sortIndex ASC")
    fun observeAll(): Flow<List<VocabEntryEntity>>

    @Query("SELECT * FROM vocab ORDER BY sortIndex ASC")
    suspend fun getAll(): List<VocabEntryEntity>

    @Query("SELECT COALESCE(MAX(sortIndex), -1) FROM vocab")
    suspend fun maxSortIndex(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VocabEntryEntity)

    @Delete
    suspend fun delete(entry: VocabEntryEntity)
}

@Dao
interface SnippetDao {
    @Query("SELECT * FROM snippets ORDER BY sortIndex ASC")
    fun observeAll(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets ORDER BY sortIndex ASC")
    suspend fun getAll(): List<SnippetEntity>

    @Query("SELECT COALESCE(MAX(sortIndex), -1) FROM snippets")
    suspend fun maxSortIndex(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SnippetEntity)

    @Delete
    suspend fun delete(entry: SnippetEntity)
}

@Dao
interface ModelDao {
    @Query("SELECT * FROM models")
    fun observeAll(): Flow<List<ModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(models: List<ModelEntity>)

    @Update
    suspend fun update(model: ModelEntity)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear(): Int

    @Query("DELETE FROM history WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int
}

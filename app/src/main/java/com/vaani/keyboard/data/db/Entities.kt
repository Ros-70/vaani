package com.vaani.keyboard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocab")
data class VocabEntryEntity(
    @PrimaryKey val id: String,
    val spoken: String,
    val written: String,
    /** Insertion order defines firing order for TextPipeline.applyVocabulary — never reorder by query. */
    val sortIndex: Int,
)

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey val id: String,
    val trigger: String,
    val text: String,
    val sortIndex: Int,
)

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sizeLabel: String,
    val langsLabel: String,
    val streaming: Boolean,
    val installed: Boolean,
    val localFileName: String?,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val text: String,
    val timestampMillis: Long,
    val appPackageName: String,
    val lang: String,
)

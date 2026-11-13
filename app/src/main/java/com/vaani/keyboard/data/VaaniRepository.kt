package com.vaani.keyboard.data

import android.content.Context
import com.vaani.keyboard.data.db.AppDatabase
import com.vaani.keyboard.data.db.HistoryEntity
import com.vaani.keyboard.data.db.ModelEntity
import com.vaani.keyboard.data.db.SnippetEntity
import com.vaani.keyboard.data.db.VocabEntryEntity
import com.vaani.keyboard.data.model.DefaultData
import com.vaani.keyboard.data.model.HistoryEntry
import com.vaani.keyboard.data.model.ModelInfo
import com.vaani.keyboard.data.model.Settings
import com.vaani.keyboard.data.model.Snippet
import com.vaani.keyboard.data.model.VocabEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Single entry point the IME and the companion app both use to read/write persisted state. */
class VaaniRepository(context: Context) {

    private val db = AppDatabase.get(context)
    val settingsStore = SettingsStore(context)

    val settings: Flow<Settings> = settingsStore.settingsFlow

    suspend fun updateSettings(transform: (Settings) -> Settings) = settingsStore.update(transform)

    // ---- Vocabulary ----

    val vocab: Flow<List<VocabEntry>> = db.vocabDao().observeAll().map { list ->
        list.map { VocabEntry(it.id, it.spoken, it.written) }
    }

    suspend fun addVocab(spoken: String, written: String) {
        val nextIndex = db.vocabDao().maxSortIndex() + 1
        db.vocabDao().insert(VocabEntryEntity(UUID.randomUUID().toString(), spoken, written, nextIndex))
    }

    suspend fun deleteVocab(entry: VocabEntry) {
        db.vocabDao().getAll().firstOrNull { it.id == entry.id }?.let { db.vocabDao().delete(it) }
    }

    // ---- Snippets ----

    val snippets: Flow<List<Snippet>> = db.snippetDao().observeAll().map { list ->
        list.map { Snippet(it.id, it.trigger, it.text) }
    }

    suspend fun addSnippet(trigger: String, text: String) {
        val nextIndex = db.snippetDao().maxSortIndex() + 1
        db.snippetDao().insert(SnippetEntity(UUID.randomUUID().toString(), trigger, text, nextIndex))
    }

    suspend fun deleteSnippet(snippet: Snippet) {
        db.snippetDao().getAll().firstOrNull { it.id == snippet.id }?.let { db.snippetDao().delete(it) }
    }

    // ---- Models ----

    val models: Flow<List<ModelInfo>> = db.modelDao().observeAll().map { list ->
        list.map { ModelInfo(it.id, it.name, it.sizeLabel, it.langsLabel, it.streaming, it.installed, it.localFileName) }
    }

    suspend fun markModelInstalled(modelId: String, localFileName: String) {
        val current = db.modelDao().observeAll().first().firstOrNull { it.id == modelId } ?: return
        db.modelDao().update(current.copy(installed = true, localFileName = localFileName))
    }

    // ---- History ----

    val history: Flow<List<HistoryEntry>> = db.historyDao().observeAll().map { list ->
        list.map { HistoryEntry(it.id, it.text, it.timestampMillis, it.appPackageName, it.lang) }
    }

    suspend fun addHistoryEntry(text: String, appPackageName: String, lang: String) {
        db.historyDao().insert(
            HistoryEntity(UUID.randomUUID().toString(), text, System.currentTimeMillis(), appPackageName, lang),
        )
        // Auto-cleared after 30 days, as described in the Settings screen's copy.
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        db.historyDao().deleteOlderThan(cutoff)
    }

    suspend fun clearHistory() = db.historyDao().clear()

    /** Settings, vocabulary, snippets and history — back to a fresh install's defaults. */
    suspend fun resetAll() {
        db.historyDao().clear()
        db.vocabDao().getAll().forEach { db.vocabDao().delete(it) }
        db.snippetDao().getAll().forEach { db.snippetDao().delete(it) }
        settingsStore.update { Settings() }
        seedDefaultsIfEmpty()
    }

    // ---- First-run seeding ----

    suspend fun seedDefaultsIfEmpty() {
        if (db.vocabDao().getAll().isEmpty()) {
            DefaultData.vocab.forEachIndexed { index, v ->
                db.vocabDao().insert(VocabEntryEntity(v.id, v.spoken, v.written, index))
            }
        }
        if (db.snippetDao().getAll().isEmpty()) {
            DefaultData.snippets.forEachIndexed { index, s ->
                db.snippetDao().insert(SnippetEntity(s.id, s.trigger, s.text, index))
            }
        }
        val existingModels = db.modelDao().observeAll().first()
        if (existingModels.isEmpty()) {
            db.modelDao().insertAll(
                DefaultData.models.map {
                    ModelEntity(it.id, it.name, it.sizeLabel, it.langsLabel, it.streaming, it.installed, it.localFileName)
                },
            )
        }
    }
}

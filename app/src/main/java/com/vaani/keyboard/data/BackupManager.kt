package com.vaani.keyboard.data

import android.content.Context
import android.net.Uri
import com.vaani.keyboard.data.model.InsertMode
import com.vaani.keyboard.data.model.MicMode
import com.vaani.keyboard.data.model.PerAppMode
import com.vaani.keyboard.data.model.Settings
import com.vaani.keyboard.data.model.ThemePreference
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private const val BACKUP_TYPE = "vaani-keyboard-backup"

sealed class RestoreResult {
    object Success : RestoreResult()
    data class Rejected(val reason: String) : RestoreResult()
}

/**
 * Exports/imports settings, vocabulary and snippets as a single JSON file,
 * tagged with [BACKUP_TYPE] so a stray unrelated JSON file is rejected with
 * a clear message rather than silently corrupting the app's data — same
 * shape as the prototype's backup format.
 */
class BackupManager(private val context: Context, private val repository: VaaniRepository) {

    suspend fun export(uri: Uri) {
        val settings = repository.settings.first()
        val vocab = repository.vocab.first()
        val snippets = repository.snippets.first()

        val payload = JSONObject().apply {
            put("type", BACKUP_TYPE)
            put("exportedAt", System.currentTimeMillis())
            put("settings", settingsToJson(settings))
            put("vocab", JSONArray().apply { vocab.forEach { put(JSONObject().put("spoken", it.spoken).put("written", it.written)) } })
            put("snippets", JSONArray().apply { snippets.forEach { put(JSONObject().put("trigger", it.trigger).put("text", it.text)) } })
        }

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(payload.toString(2).toByteArray())
        }
    }

    suspend fun import(uri: Uri): RestoreResult {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return RestoreResult.Rejected("Couldn't open that file.")

        val json = runCatching { JSONObject(text) }.getOrNull()
            ?: return RestoreResult.Rejected("That isn't a valid backup file.")

        if (json.optString("type") != BACKUP_TYPE) {
            return RestoreResult.Rejected("That isn't a Vaani backup file.")
        }

        json.optJSONObject("settings")?.let { settingsJson ->
            repository.updateSettings { current -> mergeSettings(current, settingsJson) }
        }
        json.optJSONArray("vocab")?.let { array ->
            for (i in 0 until array.length()) {
                val entry = array.getJSONObject(i)
                repository.addVocab(entry.getString("spoken"), entry.getString("written"))
            }
        }
        json.optJSONArray("snippets")?.let { array ->
            for (i in 0 until array.length()) {
                val entry = array.getJSONObject(i)
                repository.addSnippet(entry.getString("trigger"), entry.getString("text"))
            }
        }
        return RestoreResult.Success
    }

    private fun settingsToJson(settings: Settings): JSONObject = JSONObject().apply {
        put("mic", settings.mic.name)
        put("insertMode", settings.insertMode.name)
        put("autoPunct", settings.autoPunct)
        put("spokenPunct", settings.spokenPunct)
        put("autoCaps", settings.autoCaps)
        put("silenceStop", settings.silenceStop)
        put("sound", settings.sound)
        put("keepAudio", settings.keepAudio)
        put("historyEnabled", settings.historyEnabled)
        put("activeModelId", settings.activeModelId)
        put("theme", settings.theme.name)
        put("perApp", JSONObject(SettingsStore.encodePerApp(settings.perApp)))
    }

    private fun mergeSettings(current: Settings, json: JSONObject): Settings = current.copy(
        mic = runCatching { MicMode.valueOf(json.optString("mic", current.mic.name)) }.getOrDefault(current.mic),
        insertMode = runCatching { InsertMode.valueOf(json.optString("insertMode", current.insertMode.name)) }.getOrDefault(current.insertMode),
        autoPunct = json.optBoolean("autoPunct", current.autoPunct),
        spokenPunct = json.optBoolean("spokenPunct", current.spokenPunct),
        autoCaps = json.optBoolean("autoCaps", current.autoCaps),
        silenceStop = json.optBoolean("silenceStop", current.silenceStop),
        sound = json.optBoolean("sound", current.sound),
        keepAudio = json.optBoolean("keepAudio", current.keepAudio),
        historyEnabled = json.optBoolean("historyEnabled", current.historyEnabled),
        activeModelId = json.optString("activeModelId", current.activeModelId),
        theme = runCatching { ThemePreference.valueOf(json.optString("theme", current.theme.name)) }.getOrDefault(current.theme),
        perApp = json.optJSONObject("perApp")?.let { obj ->
            obj.keys().asSequence().associateWith { key ->
                runCatching { PerAppMode.valueOf(obj.getString(key)) }.getOrDefault(PerAppMode.DEFAULT)
            }
        } ?: current.perApp,
    )
}

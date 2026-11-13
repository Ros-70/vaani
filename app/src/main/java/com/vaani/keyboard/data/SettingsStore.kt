package com.vaani.keyboard.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vaani.keyboard.data.model.InsertMode
import com.vaani.keyboard.data.model.MicMode
import com.vaani.keyboard.data.model.PerAppMode
import com.vaani.keyboard.data.model.Settings
import com.vaani.keyboard.data.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.settingsDataStore by preferencesDataStore(name = "vaani_settings")

/**
 * Persists [Settings] via Jetpack DataStore (Preferences) — see DECISIONS.md
 * for why Settings uses DataStore while list data (vocab/snippets/models/
 * history) uses Room.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val MIC = stringPreferencesKey("mic")
        val INSERT_MODE = stringPreferencesKey("insert_mode")
        val AUTO_PUNCT = booleanPreferencesKey("auto_punct")
        val SPOKEN_PUNCT = booleanPreferencesKey("spoken_punct")
        val AUTO_CAPS = booleanPreferencesKey("auto_caps")
        val SILENCE_STOP = booleanPreferencesKey("silence_stop")
        val SOUND = booleanPreferencesKey("sound")
        val KEEP_AUDIO = booleanPreferencesKey("keep_audio")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val ACTIVE_MODEL_ID = stringPreferencesKey("active_model_id")
        val THEME = stringPreferencesKey("theme")
        val PER_APP_JSON = stringPreferencesKey("per_app_json")
    }

    val settingsFlow: Flow<Settings> = context.settingsDataStore.data.map(::parse)

    suspend fun update(transform: (Settings) -> Settings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(parse(prefs))
            prefs[Keys.MIC] = next.mic.name
            prefs[Keys.INSERT_MODE] = next.insertMode.name
            prefs[Keys.AUTO_PUNCT] = next.autoPunct
            prefs[Keys.SPOKEN_PUNCT] = next.spokenPunct
            prefs[Keys.AUTO_CAPS] = next.autoCaps
            prefs[Keys.SILENCE_STOP] = next.silenceStop
            prefs[Keys.SOUND] = next.sound
            prefs[Keys.KEEP_AUDIO] = next.keepAudio
            prefs[Keys.HISTORY_ENABLED] = next.historyEnabled
            prefs[Keys.ACTIVE_MODEL_ID] = next.activeModelId
            prefs[Keys.THEME] = next.theme.name
            prefs[Keys.PER_APP_JSON] = encodePerApp(next.perApp)
        }
    }

    private fun parse(prefs: Preferences): Settings = Settings(
        mic = runCatching { MicMode.valueOf(prefs[Keys.MIC] ?: MicMode.HOLD.name) }.getOrDefault(MicMode.HOLD),
        insertMode = runCatching {
            InsertMode.valueOf(prefs[Keys.INSERT_MODE] ?: InsertMode.DIRECT.name)
        }.getOrDefault(InsertMode.DIRECT),
        autoPunct = prefs[Keys.AUTO_PUNCT] ?: true,
        spokenPunct = prefs[Keys.SPOKEN_PUNCT] ?: true,
        autoCaps = prefs[Keys.AUTO_CAPS] ?: true,
        silenceStop = prefs[Keys.SILENCE_STOP] ?: true,
        sound = prefs[Keys.SOUND] ?: true,
        keepAudio = prefs[Keys.KEEP_AUDIO] ?: false,
        historyEnabled = prefs[Keys.HISTORY_ENABLED] ?: true,
        activeModelId = prefs[Keys.ACTIVE_MODEL_ID] ?: "m1",
        theme = runCatching {
            ThemePreference.valueOf(prefs[Keys.THEME] ?: ThemePreference.SYSTEM.name)
        }.getOrDefault(ThemePreference.SYSTEM),
        perApp = decodePerApp(prefs[Keys.PER_APP_JSON]),
    )

    companion object {
        fun encodePerApp(perApp: Map<String, PerAppMode>): String {
            val obj = JSONObject()
            perApp.forEach { (pkg, mode) -> obj.put(pkg, mode.name) }
            return obj.toString()
        }

        fun decodePerApp(json: String?): Map<String, PerAppMode> {
            if (json.isNullOrBlank()) return emptyMap()
            return runCatching {
                val obj = JSONObject(json)
                obj.keys().asSequence().associateWith { key ->
                    runCatching { PerAppMode.valueOf(obj.getString(key)) }.getOrDefault(PerAppMode.DEFAULT)
                }
            }.getOrDefault(emptyMap())
        }
    }
}

package com.vaani.keyboard.data.model

/** Where typed/dictated text goes before reaching the host app. */
enum class InsertMode { DIRECT, REVIEW }

/** Per-app override; DEFAULT means "follow the global insert mode". */
enum class PerAppMode { DEFAULT, DIRECT, REVIEW }

enum class MicMode { HOLD, TAP }

enum class ThemePreference { LIGHT, DARK, SYSTEM }

/** Vaani's own EN / हिं / EN+हिं language-cycle button state (not an Android IME subtype). */
enum class KeyboardLanguage { ENGLISH, HINDI, HINGLISH }

data class Settings(
    val mic: MicMode = MicMode.HOLD,
    val insertMode: InsertMode = InsertMode.DIRECT,
    val autoPunct: Boolean = true,
    val spokenPunct: Boolean = true,
    val autoCaps: Boolean = true,
    val silenceStop: Boolean = true,
    val sound: Boolean = true,
    val keepAudio: Boolean = false,
    val historyEnabled: Boolean = true,
    val activeModelId: String = "m1",
    val theme: ThemePreference = ThemePreference.SYSTEM,
    /** Keyed by real Android package name (e.g. "com.whatsapp"), not a prototype fake id. */
    val perApp: Map<String, PerAppMode> = emptyMap(),
)

data class VocabEntry(val id: String, val spoken: String, val written: String)

data class Snippet(val id: String, val trigger: String, val text: String)

data class ModelInfo(
    val id: String,
    val name: String,
    val sizeLabel: String,
    val langsLabel: String,
    val streaming: Boolean,
    /** True only once R has actually imported this model's file — never a hardcoded default. */
    val installed: Boolean,
    val localFileName: String? = null,
)

data class HistoryEntry(
    val id: String,
    val text: String,
    val timestampMillis: Long,
    val appPackageName: String,
    val lang: String,
)

object DefaultData {
    val vocab = listOf(
        VocabEntry("v1", "gem portal", "GeM portal"),
        VocabEntry("v2", "g f r", "GFR"),
        VocabEntry("v3", "n quas", "NQAS"),
        VocabEntry("v4", "lakshya", "LaQshya"),
        VocabEntry("v5", "o p d", "OPD"),
        VocabEntry("v6", "b p", "BP"),
        VocabEntry("v7", "s p o two", "SpO2"),
        VocabEntry("v8", "i c u", "ICU"),
    )

    val snippets = listOf(
        Snippet("s1", "address", "Room 214, Administrative Block,\nDistrict Hospital, New Delhi — 110001"),
        Snippet("s2", "regards", "Kind regards,\nR\nHospital Administration"),
    )

    val models = listOf(
        ModelInfo("m1", "Whisper small", "466 MB", "99 languages incl. Hindi", streaming = false, installed = false),
        ModelInfo("m2", "Whisper base", "142 MB", "99 languages incl. Hindi", streaming = false, installed = false),
        ModelInfo("m3", "Moonshine tiny", "78 MB", "English only", streaming = true, installed = false),
        ModelInfo(
            "m4",
            "IndicWhisper",
            "512 MB",
            "Hindi + Indic, Hinglish",
            streaming = false,
            installed = false,
        ),
    )
}

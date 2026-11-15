package com.vaani.keyboard.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A small in-memory ring buffer of recent log lines, copyable from Settings
 * without needing adb or a PC — ground-truth log output (not a bug report
 * written from memory) has been the only reliable way to diagnose the real
 * transcription issues reported from a real device so far.
 */
object DebugLog {
    private const val MAX_LINES = 300
    private val lines = ArrayDeque<String>()
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun log(tag: String, message: String) {
        lines.addLast("${formatter.format(Date())} $tag: $message")
        if (lines.size > MAX_LINES) lines.removeFirst()
    }

    @Synchronized
    fun dump(): String = if (lines.isEmpty()) "(no log lines yet)" else lines.joinToString("\n")
}

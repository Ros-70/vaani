package com.vaani.keyboard.ime

import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * The real `InputConnection` equivalents of the HTML prototype's simulated
 * caret/field functions — see the build brief's translation table. The host
 * app (WhatsApp, Gmail, …) owns the real text and cursor; Vaani only ever
 * reaches it through [InputConnection]. Nothing here tracks a caret position
 * locally — every operation re-derives from the real field, the same lesson
 * `currentWord()` encoded in the prototype.
 *
 * [icProvider] is a function rather than a stored reference because the
 * live `InputConnection` can change between calls (new field focused) and
 * must always be looked up fresh.
 */
class InputConnectionOps(private val icProvider: () -> InputConnection?) {

    private fun ic(): InputConnection? = icProvider()

    fun commitText(text: String) {
        ic()?.commitText(text, 1)
    }

    fun deleteBack() {
        ic()?.deleteSurroundingText(1, 0)
    }

    fun deleteCharsBack(count: Int) {
        if (count > 0) ic()?.deleteSurroundingText(count, 0)
    }

    /** Moves the caret by [delta] whole characters: negative = left, positive = right. */
    fun moveCaret(delta: Int) {
        val connection = ic() ?: return
        val keyCode = if (delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(delta)) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }

    fun sendEnter() {
        val connection = ic() ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    /** Port of the prototype's currentWord(): always derived fresh, never tracked separately. */
    fun currentWord(lookback: Int = 40): String {
        val before = ic()?.getTextBeforeCursor(lookback, 0)?.toString() ?: return ""
        val match = WORD_AT_END.find(before) ?: return ""
        return match.groupValues[1]
    }

    /** Replaces the word immediately before the caret with [pick], followed by a space. */
    fun replaceCurrentWordWith(pick: String) {
        val connection = ic() ?: return
        val word = currentWord()
        if (word.isNotEmpty()) {
            connection.deleteSurroundingText(word.length, 0)
        }
        connection.commitText("$pick ", 1)
    }

    /** Deletes the word (and any trailing whitespace) immediately before the caret. */
    fun deleteWordBack(lookback: Int = 100) {
        val connection = ic() ?: return
        val before = connection.getTextBeforeCursor(lookback, 0)?.toString() ?: return
        val match = TRAILING_WORD.find(before)
        val removeLen = match?.value?.length ?: before.length
        if (removeLen > 0) connection.deleteSurroundingText(removeLen, 0)
    }

    companion object {
        // Matches the prototype's /([A-Za-zऀ-ॿ]+)$/ — Latin + Devanagari.
        private val WORD_AT_END = Regex("([A-Za-zऀ-ॿ]+)$")

        // Matches the prototype's /\s*\S+\s*$/ used by the edit row's "⌫ word".
        private val TRAILING_WORD = Regex("\\s*\\S+\\s*$")
    }
}

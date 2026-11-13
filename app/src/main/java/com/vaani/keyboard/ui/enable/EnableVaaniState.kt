package com.vaani.keyboard.ui.enable

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.vaani.keyboard.ime.VaaniInputMethodService

enum class EnableStep { NOT_ENABLED, ENABLED_NOT_SELECTED, ACTIVE }

/**
 * Detects Vaani's keyboard-enablement state the same way the brief's point 4
 * requires — `getEnabledInputMethodList()` for "is it enabled at all",
 * `Settings.Secure.DEFAULT_INPUT_METHOD` for "is it the one currently
 * selected" — so callers never have to guess; re-check this on every resume.
 */
object EnableVaaniState {

    fun currentStep(context: Context): EnableStep {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val ownId = inputMethodId(context)

        val enabled = imm.enabledInputMethodList.any { it.id == ownId }
        if (!enabled) return EnableStep.NOT_ENABLED

        val selected = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return if (selected == ownId) EnableStep.ACTIVE else EnableStep.ENABLED_NOT_SELECTED
    }

    fun inputMethodId(context: Context): String =
        "${context.packageName}/${VaaniInputMethodService::class.java.name}"

    fun openInputMethodSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun showInputMethodPicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }
}

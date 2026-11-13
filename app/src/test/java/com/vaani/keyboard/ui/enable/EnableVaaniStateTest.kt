package com.vaani.keyboard.ui.enable

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Per the build brief's test requirement #4: covers the not-enabled /
 * enabled-but-not-selected / active states, plus the deep-link `Intent`s,
 * without requiring real navigation (that's covered by the instrumented
 * test). [EnableVaaniState.stepFor] is pure, so these need no mocked
 * `InputMethodManager`/`Settings.Secure` at all — see the refactor note on
 * that function.
 */
@RunWith(RobolectricTestRunner::class)
class EnableVaaniStateTest {

    private val ownId = "com.vaani.keyboard/com.vaani.keyboard.ime.VaaniInputMethodService"
    private val otherId = "com.google.android.inputmethod.latin/.LatinIME"

    @Test
    fun `not enabled when ownId is missing from the enabled list`() {
        val step = EnableVaaniState.stepFor(ownId, enabledIds = listOf(otherId), selectedId = otherId)
        assertEquals(EnableStep.NOT_ENABLED, step)
    }

    @Test
    fun `enabled but not selected when ownId is enabled but a different id is selected`() {
        val step = EnableVaaniState.stepFor(ownId, enabledIds = listOf(ownId, otherId), selectedId = otherId)
        assertEquals(EnableStep.ENABLED_NOT_SELECTED, step)
    }

    @Test
    fun `enabled but not selected when no default input method is set yet`() {
        val step = EnableVaaniState.stepFor(ownId, enabledIds = listOf(ownId), selectedId = null)
        assertEquals(EnableStep.ENABLED_NOT_SELECTED, step)
    }

    @Test
    fun `active when ownId is both enabled and selected`() {
        val step = EnableVaaniState.stepFor(ownId, enabledIds = listOf(otherId, ownId), selectedId = ownId)
        assertEquals(EnableStep.ACTIVE, step)
    }

    @Test
    fun `input method settings intent targets the manage-keyboards system screen`() {
        val intent = EnableVaaniState.inputMethodSettingsIntent()
        assertEquals(Settings.ACTION_INPUT_METHOD_SETTINGS, intent.action)
    }
}

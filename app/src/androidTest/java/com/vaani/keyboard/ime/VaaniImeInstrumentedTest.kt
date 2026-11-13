package com.vaani.keyboard.ime

import android.view.inputmethod.InputMethodManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.vaani.keyboard.TestEditTextActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test per the build brief's test requirement #3: this relies
 * on CI having already run
 *   adb shell ime enable com.vaani.keyboard/.ime.VaaniInputMethodService
 *   adb shell ime set    com.vaani.keyboard/.ime.VaaniInputMethodService
 * before this test class runs (see .github/workflows/android.yml — note
 * the abbreviated component id: Android's own ComponentName.
 * flattenToShortString() drops the repeated package-name prefix, so the
 * fully-qualified class name never matches). Vaani is the active system
 * keyboard once that's done, but `EditText.requestFocus()` alone doesn't
 * reliably pop the soft keyboard up in an automated test harness — this
 * explicitly calls `showSoftInput` and waits for a real Vaani key to
 * become visible before driving it with UiAutomator and asserting on the
 * committed text in the real field — not a mock.
 *
 * Vaani's keyboard surface is one long-lived service instance shared across
 * test methods within a run, so which panel (voice or QWERTY) is showing
 * when a test starts depends on where the previous test left it — never
 * assume the voice panel's "ABC ⌨" switch button is there to tap.
 */
@RunWith(AndroidJUnit4::class)
class VaaniImeInstrumentedTest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    /** Focuses the field, forces the soft keyboard up, and ensures the QWERTY panel (not voice) is showing. */
    private fun showQwertyKeyboard(scenario: ActivityScenario<TestEditTextActivity>) {
        fun requestKeyboard() {
            scenario.onActivity { activity ->
                activity.editText.requestFocus()
                val imm = activity.getSystemService(InputMethodManager::class.java)
                imm.showSoftInput(activity.editText, InputMethodManager.SHOW_FORCED)
            }
        }

        fun keyboardVisible() = device.hasObject(By.text("ABC ⌨")) || device.hasObject(By.text("q"))

        // Either panel proves the keyboard itself is on screen. BySelector has
        // no "or" composition in UiAutomator, so poll for both by hand. A
        // freshly launched Activity's window isn't always focused yet the
        // instant onActivity{} runs, so showSoftInput can silently no-op —
        // retry it rather than failing after a single attempt.
        requestKeyboard()
        var attempts = 0
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline && !keyboardVisible()) {
            Thread.sleep(300)
            attempts++
            if (attempts % 10 == 0) requestKeyboard() // retry roughly every 3s
        }
        assertTrue("Vaani's keyboard never appeared on screen", keyboardVisible())
        // Only the voice panel needs switching; a prior test may have left QWERTY showing already.
        device.findObject(By.text("ABC ⌨"))?.click()
        assertNotNull("QWERTY panel never appeared", device.wait(Until.findObject(By.text("q")), 3000))
    }

    private fun tap(label: String): UiObject2 {
        val obj = device.wait(Until.findObject(By.text(label)), 3000)
        assertNotNull("Key \"$label\" never appeared on the keyboard", obj)
        obj.click()
        return obj
    }

    @Test
    fun typing_q_w_e_on_the_letters_layer_commits_qwe_to_the_real_field() {
        ActivityScenario.launch(TestEditTextActivity::class.java).use { scenario ->
            showQwertyKeyboard(scenario)

            tap("q")
            tap("w")
            tap("e")

            scenario.onActivity { activity ->
                assertEquals("qwe", activity.editText.text.toString())
            }
        }
    }

    @Test
    fun backspace_removes_the_last_committed_character() {
        ActivityScenario.launch(TestEditTextActivity::class.java).use { scenario ->
            showQwertyKeyboard(scenario)

            tap("a")
            tap("b")
            tap("⌫")

            scenario.onActivity { activity ->
                assertEquals("a", activity.editText.text.toString())
            }
        }
    }
}

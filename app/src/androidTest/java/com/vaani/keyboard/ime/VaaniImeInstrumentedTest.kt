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
 */
@RunWith(AndroidJUnit4::class)
class VaaniImeInstrumentedTest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    /** Focuses the field, forces the soft keyboard up, and waits for Vaani's voice panel to render. */
    private fun showKeyboard(scenario: ActivityScenario<TestEditTextActivity>) {
        scenario.onActivity { activity ->
            activity.editText.requestFocus()
            val imm = activity.getSystemService(InputMethodManager::class.java)
            imm.showSoftInput(activity.editText, InputMethodManager.SHOW_FORCED)
        }
        assertNotNull(
            "Vaani's keyboard never appeared on screen",
            device.wait(Until.findObject(By.text("ABC ⌨")), 8000),
        )
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
            showKeyboard(scenario)

            tap("ABC ⌨") // switch off the voice panel onto QWERTY
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
            showKeyboard(scenario)

            tap("ABC ⌨")
            tap("a")
            tap("b")
            tap("⌫")

            scenario.onActivity { activity ->
                assertEquals("a", activity.editText.text.toString())
            }
        }
    }
}

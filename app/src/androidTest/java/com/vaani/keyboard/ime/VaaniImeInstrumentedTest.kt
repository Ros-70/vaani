package com.vaani.keyboard.ime

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.vaani.keyboard.TestEditTextActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test per the build brief's test requirement #3: this relies
 * on CI having already run
 *   adb shell ime enable com.vaani.keyboard/com.vaani.keyboard.ime.VaaniInputMethodService
 *   adb shell ime set    com.vaani.keyboard/com.vaani.keyboard.ime.VaaniInputMethodService
 * before this test class runs (see .github/workflows/android.yml), so Vaani
 * is the active system keyboard when the test Activity's `EditText` gets
 * focus. It then drives Vaani's real on-screen keys with UiAutomator and
 * asserts on the committed text in the real field — not a mock.
 */
@RunWith(AndroidJUnit4::class)
class VaaniImeInstrumentedTest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun typing_q_w_e_on_the_letters_layer_commits_qwe_to_the_real_field() {
        ActivityScenario.launch(TestEditTextActivity::class.java).use { scenario ->
            scenario.onActivity { it.editText.requestFocus() }
            device.waitForIdle()

            // Switch off the voice panel onto QWERTY before typing letters.
            device.findObject(By.text("ABC ⌨"))?.click()
            device.wait(Until.hasObject(By.text("q")), 3000)

            device.findObject(By.text("q"))?.click()
            device.findObject(By.text("w"))?.click()
            device.findObject(By.text("e"))?.click()

            scenario.onActivity { activity ->
                assertEquals("qwe", activity.editText.text.toString())
            }
        }
    }

    @Test
    fun backspace_removes_the_last_committed_character() {
        ActivityScenario.launch(TestEditTextActivity::class.java).use { scenario ->
            scenario.onActivity { it.editText.requestFocus() }
            device.waitForIdle()

            device.findObject(By.text("ABC ⌨"))?.click()
            device.wait(Until.hasObject(By.text("a")), 3000)
            device.findObject(By.text("a"))?.click()
            device.findObject(By.text("b"))?.click()
            device.findObject(By.text("⌫"))?.click()

            scenario.onActivity { activity ->
                assertEquals("a", activity.editText.text.toString())
            }
        }
    }
}

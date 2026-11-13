package com.vaani.keyboard.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.vaani.keyboard.VaaniApplication
import com.vaani.keyboard.ime.compose.KeyboardSurface
import com.vaani.keyboard.speech.AudioCapture
import com.vaani.keyboard.speech.WhisperEngine
import com.vaani.keyboard.ui.MainActivity
import com.vaani.keyboard.ui.theme.VaaniTheme
import com.vaani.keyboard.ui.theme.VaaniThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * The system keyboard service. Per the build brief, this is the one piece
 * that MUST be a native Android `InputMethodService` — no supported,
 * production-stable way exists to make a Flutter surface serve as the
 * actual system keyboard view.
 */
class VaaniInputMethodService : InputMethodService() {

    private lateinit var imeLifecycleOwner: ImeLifecycleOwner
    private lateinit var serviceScope: CoroutineScope
    private lateinit var keyboardEngine: KeyboardEngine
    private lateinit var ops: InputConnectionOps

    override fun onCreate() {
        super.onCreate()
        imeLifecycleOwner = ImeLifecycleOwner().also {
            it.performRestore()
            it.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        ops = InputConnectionOps { currentInputConnection }

        val app = application as VaaniApplication
        keyboardEngine = KeyboardEngine(
            repository = app.repository,
            speechEngine = WhisperEngine(this),
            audioCapture = AudioCapture(this),
            ops = ops,
            scope = serviceScope,
            onOpenCompanion = { screen -> openCompanionApp(screen) },
        )
        keyboardEngine.start()
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(imeLifecycleOwner)
            setViewTreeViewModelStoreOwner(imeLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(imeLifecycleOwner)
            setContent {
                VaaniTheme(mode = VaaniThemeMode.SYSTEM) {
                    KeyboardSurface(keyboardEngine)
                }
            }
        }
        imeLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        imeLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return composeView
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val isPassword = isPasswordField(info.inputType)
        keyboardEngine.editorContext = EditorContext(
            packageName = info.packageName ?: "",
            isPasswordField = isPassword,
        )
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        keyboardEngine.stop()
        serviceScope.cancel()
        imeLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    private fun isPasswordField(inputType: Int): Boolean {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val isNumberPassword = (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER &&
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            isNumberPassword
    }

    private fun openCompanionApp(screen: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_SCREEN, screen)
        }
        startActivity(intent)
    }
}

package com.vaani.keyboard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.vaani.keyboard.VaaniApplication
import com.vaani.keyboard.ui.enable.EnableStep
import com.vaani.keyboard.ui.enable.EnableVaaniState
import com.vaani.keyboard.ui.theme.VaaniTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_SCREEN = "open_screen"
    }

    private var enableStep by mutableStateOf(EnableStep.NOT_ENABLED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableStep = EnableVaaniState.currentStep(this)

        // Re-check automatically on every resume — same LifecycleObserver pattern
        // used elsewhere in R's apps for permission/enablement state — rather
        // than making R confirm manually that they've come back.
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                enableStep = EnableVaaniState.currentStep(this@MainActivity)
            }
        })

        val repository = (application as VaaniApplication).repository
        val initialScreen = intent.getStringExtra(EXTRA_OPEN_SCREEN)

        setContent {
            VaaniTheme {
                VaaniApp(
                    repository = repository,
                    enableStep = enableStep,
                    initialScreen = initialScreen,
                )
            }
        }
    }
}

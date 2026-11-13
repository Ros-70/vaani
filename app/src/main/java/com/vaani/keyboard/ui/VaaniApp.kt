package com.vaani.keyboard.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaani.keyboard.data.VaaniRepository
import com.vaani.keyboard.ui.enable.EnableStep
import com.vaani.keyboard.ui.screens.BackupScreen
import com.vaani.keyboard.ui.screens.HistoryScreen
import com.vaani.keyboard.ui.screens.HomeScreen
import com.vaani.keyboard.ui.screens.ModelsScreen
import com.vaani.keyboard.ui.screens.PrivacyScreen
import com.vaani.keyboard.ui.screens.SettingsScreen
import com.vaani.keyboard.ui.screens.SnippetsScreen
import com.vaani.keyboard.ui.screens.VocabularyScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val VOCABULARY = "vocabulary"
    const val SNIPPETS = "snippets"
    const val HISTORY = "history"
    const val MODELS = "models"
    const val PRIVACY = "privacy"
    const val BACKUP = "backup"
}

@Composable
fun VaaniApp(
    repository: VaaniRepository,
    enableStep: EnableStep,
    initialScreen: String?,
) {
    val navController = rememberNavController()
    var showMicRationale by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(initialScreen) {
        when (initialScreen) {
            "settings" -> navController.navigate(Routes.SETTINGS)
            "snippets" -> navController.navigate(Routes.SNIPPETS)
            "history" -> navController.navigate(Routes.HISTORY)
            "request_mic_permission" -> showMicRationale = true
        }
    }

    if (showMicRationale) {
        AlertDialog(
            onDismissRequest = { showMicRationale = false },
            title = { Text("Microphone access") },
            // The one plain sentence the build brief asks for, shown before
            // the system permission dialog itself — not a substitute for it.
            text = { Text("Vaani needs the microphone to turn your speech into text — entirely on this phone, nothing is sent anywhere.") },
            confirmButton = {
                TextButton(onClick = {
                    showMicRationale = false
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showMicRationale = false }) { Text("Not now") }
            },
        )
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                enableStep = enableStep,
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        composable(Routes.SETTINGS) { SettingsScreen(repository, navController) }
        composable(Routes.VOCABULARY) { VocabularyScreen(repository, navController) }
        composable(Routes.SNIPPETS) { SnippetsScreen(repository, navController) }
        composable(Routes.HISTORY) { HistoryScreen(repository, navController) }
        composable(Routes.MODELS) { ModelsScreen(repository, navController) }
        composable(Routes.PRIVACY) { PrivacyScreen(navController) }
        composable(Routes.BACKUP) { BackupScreen(repository, navController) }
    }
}

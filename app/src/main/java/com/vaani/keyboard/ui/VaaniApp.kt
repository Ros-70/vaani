package com.vaani.keyboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    LaunchedEffect(initialScreen) {
        when (initialScreen) {
            "settings" -> navController.navigate(Routes.SETTINGS)
            "snippets" -> navController.navigate(Routes.SNIPPETS)
            "history" -> navController.navigate(Routes.HISTORY)
        }
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

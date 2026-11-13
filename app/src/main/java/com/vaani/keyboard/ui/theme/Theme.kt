package com.vaani.keyboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalVaaniColors = compositionLocalOf { LightVaaniColors }

enum class VaaniThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun VaaniTheme(
    mode: VaaniThemeMode = VaaniThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        VaaniThemeMode.LIGHT -> false
        VaaniThemeMode.DARK -> true
        VaaniThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (dark) DarkVaaniColors else LightVaaniColors

    val material = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.accentInk,
            background = palette.bg,
            onBackground = palette.ink,
            surface = palette.card,
            onSurface = palette.ink,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.accentInk,
            background = palette.bg,
            onBackground = palette.ink,
            surface = palette.card,
            onSurface = palette.ink,
            error = palette.danger,
        )
    }

    CompositionLocalProvider(LocalVaaniColors provides palette) {
        MaterialTheme(colorScheme = material, content = content)
    }
}

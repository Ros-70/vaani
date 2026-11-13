package com.vaani.keyboard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Centralized color tokens ported from vaani-keyboard-v1_0.html's CSS
 * variables. No raw color literals belong anywhere else in the app —
 * everything reads from here via [VaaniColors]/[LocalVaaniColors].
 */
data class VaaniColorPalette(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val sub: Color,
    val line: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val danger: Color,
    val good: Color,
    val warn: Color,
    val keyboardBg: Color,
    val key: Color,
    val keyAlt: Color,
    val keyInk: Color,
)

val LightVaaniColors = VaaniColorPalette(
    bg = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF15151C),
    sub = Color(0xFF6B6B7B),
    line = Color(0xFFE3E3EB),
    accent = Color(0xFF5B4FD6),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFECEAFC),
    danger = Color(0xFFC33A2E),
    good = Color(0xFF1C7A4D),
    warn = Color(0xFF9A6A10),
    keyboardBg = Color(0xFFD7D7E0),
    key = Color(0xFFFFFFFF),
    keyAlt = Color(0xFFBFBFCC),
    keyInk = Color(0xFF15151C),
)

val DarkVaaniColors = VaaniColorPalette(
    bg = Color(0xFF0E0E13),
    card = Color(0xFF1A1A22),
    ink = Color(0xFFEDEDF3),
    sub = Color(0xFF9999AA),
    line = Color(0xFF2B2B37),
    accent = Color(0xFF8D81F2),
    accentInk = Color(0xFF12121A),
    accentSoft = Color(0xFF262043),
    danger = Color(0xFFFF6F61),
    good = Color(0xFF4FD18B),
    warn = Color(0xFFE5B65A),
    keyboardBg = Color(0xFF14141B),
    key = Color(0xFF2B2B37),
    keyAlt = Color(0xFF1E1E27),
    keyInk = Color(0xFFEDEDF3),
)

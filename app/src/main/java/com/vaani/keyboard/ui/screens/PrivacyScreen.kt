package com.vaani.keyboard.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.vaani.keyboard.ui.components.Card
import com.vaani.keyboard.ui.components.ScreenScaffold
import com.vaani.keyboard.ui.theme.LocalVaaniColors

private data class PermLine(val mark: String, val title: String, val badge: String, val badgeGood: Boolean, val body: String)

private val lines = listOf(
    PermLine("🚫", "Internet access", "NOT REQUESTED", true, "Vaani does not hold the INTERNET permission. It is structurally incapable of sending anything anywhere."),
    PermLine("🎙", "Microphone", "WHILE DICTATING", false, "Live only while you hold (or tap to start) the mic. Audio is transcribed on this phone and discarded unless you turn on \"Keep recorded audio\"."),
    PermLine("🚫", "Clipboard", "NEVER READ", true, "Unlike most keyboards, Vaani never inspects what you copy."),
    PermLine("📂", "Storage", "ON REQUEST", false, "Only when you pick a model file to import, or export/import a backup — never broad storage access."),
    PermLine("🚫", "Analytics & crash reporting", "NONE", true, "No telemetry of any kind is collected or sent."),
    PermLine("📒", "What you type and say", "STAYS HERE", true, "Vocabulary, snippets and history live in this app's private storage and are deletable at any time."),
)

@Composable
fun PrivacyScreen(navController: NavHostController) {
    val colors = LocalVaaniColors.current
    ScreenScaffold(
        title = "What this app can and cannot do",
        subtitle = "Every line here is checkable in Android's own app info screen.",
        onBack = { navController.popBackStack() },
    ) {
        Card {
            lines.forEachIndexed { index, line ->
                Row {
                    Text(line.mark, modifier = Modifier)
                    Spacer(modifier = Modifier.height(0.dp))
                    androidx.compose.foundation.layout.Column {
                        Text(line.title, color = colors.ink)
                        Text(
                            line.badge,
                            color = if (line.badgeGood) colors.good else colors.warn,
                        )
                        Text(line.body, color = colors.sub)
                    }
                }
                if (index != lines.lastIndex) Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

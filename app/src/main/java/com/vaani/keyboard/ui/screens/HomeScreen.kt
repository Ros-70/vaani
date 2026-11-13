package com.vaani.keyboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaani.keyboard.ui.Routes
import com.vaani.keyboard.ui.components.ListRow
import com.vaani.keyboard.ui.enable.EnableStep
import com.vaani.keyboard.ui.enable.EnableVaaniState
import com.vaani.keyboard.ui.theme.LocalVaaniColors

private data class NavItem(val route: String, val emoji: String, val title: String, val subtitle: String)

private val navItems = listOf(
    NavItem(Routes.SETTINGS, "⚙️", "Settings", "Mic behaviour, insert mode, per-app overrides"),
    NavItem(Routes.VOCABULARY, "📒", "Vocabulary", "Words the model keeps getting wrong"),
    NavItem(Routes.SNIPPETS, "⚡", "Snippets", "Stock text you send often"),
    NavItem(Routes.HISTORY, "🕘", "History", "What you've dictated recently"),
    NavItem(Routes.MODELS, "🧠", "Speech models", "Import and choose a speech model"),
    NavItem(Routes.PRIVACY, "🔒", "Privacy", "What this app can and cannot do"),
    NavItem(Routes.BACKUP, "💾", "Backup & restore", "Save or restore your settings"),
)

@Composable
fun HomeScreen(enableStep: EnableStep, onNavigate: (String) -> Unit) {
    val colors = LocalVaaniColors.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(16.dp)) {
        Text("Vaani", color = colors.ink, fontWeight = FontWeight.Bold)
        Text("Everything here stays on this phone.", color = colors.sub)

        if (enableStep != EnableStep.ACTIVE) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.accentSoft)
                    .padding(14.dp),
            ) {
                when (enableStep) {
                    EnableStep.NOT_ENABLED -> {
                        Text("Turn on Vaani", color = colors.accent, fontWeight = FontWeight.Bold)
                        Text(
                            "Vaani isn't turned on as a keyboard yet. Tap below to open Android's keyboard list, then switch Vaani on.",
                            color = colors.ink,
                        )
                        Button(
                            onClick = { EnableVaaniState.openInputMethodSettings(context) },
                            modifier = Modifier.padding(top = 10.dp),
                        ) { Text("Open keyboard settings") }
                    }
                    EnableStep.ENABLED_NOT_SELECTED -> {
                        Text("Almost there", color = colors.accent, fontWeight = FontWeight.Bold)
                        Text(
                            "Vaani is turned on. Now tap below to pick it as the keyboard you're typing with right now.",
                            color = colors.ink,
                        )
                        Button(
                            onClick = { EnableVaaniState.showInputMethodPicker(context) },
                            modifier = Modifier.padding(top = 10.dp),
                        ) { Text("Choose Vaani as my keyboard") }
                    }
                    EnableStep.ACTIVE -> Unit
                }
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text("✓ Vaani is set up and ready.", color = colors.good)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(navItems) { item ->
                ListRow(title = "${item.emoji} ${item.title}", subtitle = item.subtitle) {
                    Button(onClick = { onNavigate(item.route) }) { Text("Open") }
                }
            }
        }
    }
}

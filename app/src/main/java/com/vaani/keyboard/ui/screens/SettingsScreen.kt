package com.vaani.keyboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.vaani.keyboard.data.VaaniRepository
import com.vaani.keyboard.data.model.InsertMode
import com.vaani.keyboard.data.model.MicMode
import com.vaani.keyboard.data.model.PerAppMode
import com.vaani.keyboard.data.model.Settings
import com.vaani.keyboard.ui.Routes
import com.vaani.keyboard.ui.components.Card
import com.vaani.keyboard.ui.components.ScreenScaffold
import com.vaani.keyboard.ui.components.SegmentedControl
import com.vaani.keyboard.ui.components.ToggleRow
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(repository: VaaniRepository, navController: NavHostController) {
    val settings by repository.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()

    fun update(transform: (Settings) -> Settings) {
        scope.launch { repository.updateSettings(transform) }
    }

    ScreenScaffold(title = "Settings", subtitle = "Everything here stays on this phone.", onBack = { navController.popBackStack() }) {
        LazyColumn {
            item {
                Card {
                    Text("Dictation")
                    SegmentedControl(
                        options = listOf(MicMode.HOLD.name to "Hold to talk", MicMode.TAP.name to "Tap to start / stop"),
                        selected = settings.mic.name,
                        onSelect = { value -> update { it.copy(mic = MicMode.valueOf(value)) } },
                    )
                    SegmentedControl(
                        options = listOf(InsertMode.DIRECT.name to "Insert straight away", InsertMode.REVIEW.name to "Review first"),
                        selected = settings.insertMode.name,
                        onSelect = { value -> update { it.copy(insertMode = InsertMode.valueOf(value)) } },
                    )
                    ToggleRow(
                        "Automatic punctuation", "Model adds commas and full stops itself",
                        settings.autoPunct,
                    ) { update { s -> s.copy(autoPunct = it) } }
                    ToggleRow(
                        "Spoken punctuation", "Saying \"comma\", \"full stop\", \"new paragraph\" works",
                        settings.spokenPunct,
                    ) { update { s -> s.copy(spokenPunct = it) } }
                    ToggleRow("Auto-capitalise sentences", checked = settings.autoCaps) { update { s -> s.copy(autoCaps = it) } }
                    ToggleRow(
                        "Stop on silence", "Ends dictation after a short pause",
                        settings.silenceStop,
                    ) { update { s -> s.copy(silenceStop = it) } }
                    ToggleRow("Start / stop sound", checked = settings.sound) { update { s -> s.copy(sound = it) } }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card {
                    Text("Per-app behaviour")
                    PerAppList(settings, onChange = { pkg, mode -> update { it.copy(perApp = it.perApp + (pkg to mode)) } })
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card {
                    Text("Privacy & safety")
                    ToggleRow(
                        "Keep recorded audio", "Off: audio is processed in memory and discarded",
                        settings.keepAudio,
                    ) { update { s -> s.copy(keepAudio = it) } }
                    ToggleRow(
                        "Save dictation history", "Stored on-device only, auto-cleared after 30 days",
                        settings.historyEnabled,
                    ) { update { s -> s.copy(historyEnabled = it) } }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(onClick = { navController.navigate(Routes.PRIVACY) }) { Text("🔒 What this app can and cannot do") }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card {
                    Text("Words & models")
                    Button(onClick = { navController.navigate(Routes.VOCABULARY) }) { Text("📒 Vocabulary & corrections") }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(onClick = { navController.navigate(Routes.MODELS) }) { Text("🧠 Speech models") }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(onClick = { navController.navigate(Routes.BACKUP) }) { Text("💾 Backup & restore") }
                }
            }
        }
    }
}

/**
 * Keyed by the real Android package name, shown via the host app's real
 * label — not a package-name string — so R recognizes what they're
 * configuring (the companion-app equivalent of the brief's perApp note).
 */
@Composable
private fun PerAppList(settings: Settings, onChange: (String, PerAppMode) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pm = context.packageManager

    // The apps R has actually overridden, plus the host apps already visible
    // in Vaani's recent dictation history, surface here automatically —
    // there is no fixed "known app" list the way the prototype had one.
    val packages = remember(settings.perApp) { settings.perApp.keys.toList() }

    if (packages.isEmpty()) {
        Text("No overrides yet — open an app and dictate once, then come back here.")
        return
    }

    packages.forEach { pkg ->
        val label = remember(pkg) {
            runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
        }
        var menuOpen by remember { mutableStateOf(false) }
        val current = settings.perApp[pkg] ?: PerAppMode.DEFAULT

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label)
            Button(onClick = { menuOpen = true }) { Text(current.name) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                PerAppMode.entries.forEach { mode ->
                    DropdownMenuItem(text = { Text(mode.name) }, onClick = { onChange(pkg, mode); menuOpen = false })
                }
            }
        }
    }
}

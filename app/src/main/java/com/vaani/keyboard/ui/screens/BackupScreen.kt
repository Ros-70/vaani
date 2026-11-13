package com.vaani.keyboard.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.vaani.keyboard.data.BackupManager
import com.vaani.keyboard.data.RestoreResult
import com.vaani.keyboard.data.VaaniRepository
import com.vaani.keyboard.ui.components.Card
import com.vaani.keyboard.ui.components.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(repository: VaaniRepository, navController: NavHostController) {
    val context = LocalContext.current
    val backupManager = remember { BackupManager(context, repository) }
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupManager.export(uri)
            statusMessage = "Backup exported."
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            statusMessage = when (val result = backupManager.import(uri)) {
                is RestoreResult.Success -> "Backup restored."
                is RestoreResult.Rejected -> result.reason
            }
        }
    }

    ScreenScaffold(
        title = "Backup & restore",
        subtitle = "Keep your vocabulary, snippets and settings safe across reinstalls or a new phone.",
        onBack = { navController.popBackStack() },
    ) {
        statusMessage?.let { Text(it); Spacer(modifier = Modifier.height(8.dp)) }

        Card {
            Text("Export")
            Spacer(modifier = Modifier.height(6.dp))
            Button(onClick = { exportLauncher.launch("vaani-backup.json") }) {
                Text("Export settings, vocabulary & snippets")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Card {
            Text("Import")
            Spacer(modifier = Modifier.height(6.dp))
            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                Text("Restore from a backup file")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Card {
            Text("Danger zone")
            Spacer(modifier = Modifier.height(6.dp))
            Button(onClick = { showResetConfirm = true }) {
                Text("Reset everything")
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset everything?") },
            text = { Text("Settings, vocabulary, snippets and history will all be reset.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    scope.launch { repository.resetAll(); statusMessage = "Everything reset." }
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

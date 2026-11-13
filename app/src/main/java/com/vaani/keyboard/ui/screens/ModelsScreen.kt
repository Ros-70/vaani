package com.vaani.keyboard.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.vaani.keyboard.data.VaaniRepository
import com.vaani.keyboard.data.model.ModelInfo
import com.vaani.keyboard.speech.ModelImportManager
import com.vaani.keyboard.speech.ModelImportResult
import com.vaani.keyboard.ui.components.ListRow
import com.vaani.keyboard.ui.components.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun ModelsScreen(repository: VaaniRepository, navController: NavHostController) {
    val models by repository.models.collectAsState(initial = emptyList())
    val settings by repository.settings.collectAsState(initial = com.vaani.keyboard.data.model.Settings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val importManager = remember { ModelImportManager(context) }

    var importingModelId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingModel by remember { mutableStateOf<ModelInfo?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val model = pendingModel
        pendingModel = null
        if (uri == null || model == null) return@rememberLauncherForActivityResult
        importingModelId = model.id
        scope.launch {
            val fileName = "${model.id}.bin"
            val result = importManager.import(uri, fileName)
            importingModelId = null
            statusMessage = when (result) {
                is ModelImportResult.Success -> {
                    repository.markModelInstalled(model.id, result.fileName)
                    "${model.name} imported."
                }
                is ModelImportResult.Rejected -> result.reason
            }
        }
    }

    ScreenScaffold(
        title = "Speech models",
        subtitle = "Models are imported from a file you download yourself. Vaani has no internet permission, so it can never fetch anything on its own.",
        onBack = { navController.popBackStack() },
    ) {
        statusMessage?.let { Text(it); Spacer(modifier = Modifier.height(8.dp)) }

        LazyColumn {
            items(models, key = { it.id }) { model ->
                ListRow(
                    title = model.name + if (model.streaming) " · live text" else "",
                    subtitle = model.sizeLabel + " · " + model.langsLabel + if (!model.installed) " · not imported" else "",
                ) {
                    when {
                        importingModelId == model.id -> CircularProgressIndicator(modifier = Modifier.height(24.dp))
                        model.installed && settings.activeModelId == model.id -> Text("✓ In use")
                        model.installed -> Button(onClick = {
                            scope.launch { repository.updateSettings { it.copy(activeModelId = model.id) } }
                        }) { Text("Use") }
                        else -> Button(onClick = {
                            pendingModel = model
                            pickerLauncher.launch(arrayOf("*/*"))
                        }) { Text("Import…") }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {
            // The system browser does the fetching here, not Vaani — this needs
            // no INTERNET permission in Vaani's own manifest (see HARD
            // REQUIREMENTS #1's one documented exception).
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                Uri.parse("https://huggingface.co/ggerganov/whisper.cpp"),
            )
            context.startActivity(intent)
        }) { Text("📂 Where to find a model") }
    }
}

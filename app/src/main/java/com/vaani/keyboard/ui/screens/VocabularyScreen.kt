package com.vaani.keyboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import com.vaani.keyboard.data.model.VocabEntry
import com.vaani.keyboard.ui.components.Card
import com.vaani.keyboard.ui.components.EmptyState
import com.vaani.keyboard.ui.components.ListRow
import com.vaani.keyboard.ui.components.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun VocabularyScreen(repository: VaaniRepository, navController: NavHostController) {
    val vocab by repository.vocab.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var spoken by remember { mutableStateOf("") }
    var written by remember { mutableStateOf("") }

    ScreenScaffold(
        title = "Vocabulary & corrections",
        subtitle = "Words the model keeps getting wrong. Applied after transcription, so these work with every model.",
        onBack = { navController.popBackStack() },
    ) {
        Card {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = spoken, onValueChange = { spoken = it }, label = { Text("When it hears") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = written, onValueChange = { written = it }, label = { Text("Write instead") }, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (spoken.isNotBlank() && written.isNotBlank()) {
                    scope.launch { repository.addVocab(spoken.trim(), written.trim()) }
                    spoken = ""
                    written = ""
                }
            }) { Text("Add correction") }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (vocab.isEmpty()) {
            EmptyState("📒", "No corrections yet.")
        } else {
            LazyColumn {
                items(vocab, key = { it.id }) { entry: VocabEntry ->
                    ListRow(title = entry.written, subtitle = "heard as \"${entry.spoken}\"") {
                        Button(onClick = { scope.launch { repository.deleteVocab(entry) } }) { Text("Delete") }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

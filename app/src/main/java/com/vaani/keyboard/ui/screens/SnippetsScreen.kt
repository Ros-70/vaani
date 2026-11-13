package com.vaani.keyboard.ui.screens

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
import com.vaani.keyboard.ui.components.Card
import com.vaani.keyboard.ui.components.EmptyState
import com.vaani.keyboard.ui.components.ListRow
import com.vaani.keyboard.ui.components.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun SnippetsScreen(repository: VaaniRepository, navController: NavHostController) {
    val snippets by repository.snippets.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var trigger by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    ScreenScaffold(
        title = "Snippets",
        subtitle = "Stock text you send often. Tap one to insert it, or say \"insert\" followed by its name.",
        onBack = { navController.popBackStack() },
    ) {
        Card {
            OutlinedTextField(value = trigger, onValueChange = { trigger = it }, label = { Text("Name / trigger") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Full text") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (trigger.isNotBlank() && text.isNotBlank()) {
                    scope.launch { repository.addSnippet(trigger.trim(), text.trim()) }
                    trigger = ""
                    text = ""
                }
            }) { Text("Add snippet") }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (snippets.isEmpty()) {
            EmptyState("⚡", "No snippets yet.")
        } else {
            LazyColumn {
                items(snippets, key = { it.id }) { snippet ->
                    ListRow(title = snippet.trigger, subtitle = snippet.text.replace("\n", " ⏎ ").take(80)) {
                        Button(onClick = { scope.launch { repository.deleteSnippet(snippet) } }) { Text("✕") }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

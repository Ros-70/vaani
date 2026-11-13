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
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(repository: VaaniRepository, navController: NavHostController) {
    val history by repository.history.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    val filtered = remember(history, query) {
        if (query.isBlank()) history else history.filter { it.text.contains(query, ignoreCase = true) }
    }

    ScreenScaffold(
        title = "Dictation history",
        subtitle = "On this phone only. Respects the history toggle in Settings.",
        onBack = { navController.popBackStack() },
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search history…") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            EmptyState("🕘", "Nothing dictated yet.")
        } else {
            LazyColumn {
                items(filtered, key = { it.id }) { entry ->
                    ListRow(
                        title = entry.text.take(110),
                        subtitle = "${entry.appPackageName} · ${DateFormat.getDateTimeInstance().format(Date(entry.timestampMillis))}",
                    ) {}
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Card {
            Button(onClick = { scope.launch { repository.clearHistory() } }) { Text("Clear all history") }
        }
    }
}

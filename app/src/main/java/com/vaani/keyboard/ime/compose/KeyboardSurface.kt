package com.vaani.keyboard.ime.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaani.keyboard.data.model.KeyboardLanguage
import com.vaani.keyboard.ime.KeyLayer
import com.vaani.keyboard.ime.KeyboardEngine
import com.vaani.keyboard.ime.KeyboardPanel
import com.vaani.keyboard.ui.theme.LocalVaaniColors

@Composable
fun KeyboardSurface(engine: KeyboardEngine) {
    val colors = LocalVaaniColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.keyboardBg),
    ) {
        if (engine.reviewBarOpen) {
            ReviewBar(engine)
        }
        KeyboardTopBar(engine)
        SuggestionStrip(engine)
        when (engine.panel) {
            KeyboardPanel.VOICE -> VoicePanel(engine)
            KeyboardPanel.QWERTY -> QwertyPanel(engine)
        }
    }
}

@Composable
private fun KeyboardTopBar(engine: KeyboardEngine) {
    val colors = LocalVaaniColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Button(
            onClick = { engine.cycleLanguage() },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.accentInk),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        ) {
            Text(languageLabel(engine))
        }
        Text(
            text = "offline",
            color = colors.sub,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = { engine.openHistory() }) {
            Icon(Icons.Filled.History, contentDescription = "History", tint = colors.sub)
        }
        IconButton(onClick = { engine.openSnippets() }) {
            Icon(Icons.Filled.Bolt, contentDescription = "Snippets", tint = colors.sub)
        }
        IconButton(onClick = { engine.openSettings() }) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = colors.sub)
        }
    }
}

private fun languageLabel(engine: KeyboardEngine): String = when (engine.language) {
    KeyboardLanguage.ENGLISH -> "EN"
    KeyboardLanguage.HINDI -> "हिं"
    KeyboardLanguage.HINGLISH -> "EN+हिं"
}

@Composable
private fun ReviewBar(engine: KeyboardEngine) {
    val colors = LocalVaaniColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.warn.copy(alpha = 0.12f))
            .padding(8.dp),
    ) {
        Text("REVIEW BEFORE INSERTING", color = colors.warn)
        BasicTextField(
            value = engine.reviewText,
            onValueChange = { engine.reviewText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.card)
                .padding(8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
            Button(onClick = { engine.onReviewInsert() }) { Text("Insert") }
            OutlinedButton(onClick = { engine.onReviewRedictate() }) { Text("↺ Redo") }
            OutlinedButton(onClick = { engine.onReviewDiscard() }) { Text("Discard") }
        }
    }
}

@Composable
private fun SuggestionStrip(engine: KeyboardEngine) {
    val colors = LocalVaaniColors.current
    if (engine.panel != KeyboardPanel.QWERTY) {
        Row(modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔒 Offline · nothing is sent anywhere", color = colors.sub)
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (engine.suggestionWord.isNotEmpty()) {
            SuggestionChip(engine.suggestionWord, isPick = true, colors = colors) {}
            engine.suggestionCandidates.forEach { candidate ->
                SuggestionChip(candidate, isPick = false, colors = colors) { engine.onSuggestionPick(candidate) }
            }
            if (engine.suggestionCandidates.isEmpty()) {
                Text(
                    if (engine.language == KeyboardLanguage.ENGLISH) "suggestions are local" else "type in Latin → Devanagari",
                    color = colors.sub,
                )
            }
        } else {
            Text("🔒 Offline · no word leaves this phone", color = colors.sub)
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    isPick: Boolean,
    colors: com.vaani.keyboard.ui.theme.VaaniColorPalette,
    onClick: () -> Unit,
) {
    var modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(if (isPick) colors.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
    if (!isPick) {
        modifier = modifier.clickable(onClick = onClick)
    }
    Box(modifier = modifier.padding(horizontal = 11.dp, vertical = 4.dp)) {
        Text(text = text, color = if (isPick) colors.accent else colors.ink)
    }
}

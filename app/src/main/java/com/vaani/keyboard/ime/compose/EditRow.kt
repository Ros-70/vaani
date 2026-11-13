package com.vaani.keyboard.ime.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vaani.keyboard.ime.KeyboardEngine
import com.vaani.keyboard.ui.theme.LocalVaaniColors

@Composable
fun EditRow(engine: KeyboardEngine) {
    val colors = LocalVaaniColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        EditKey("◀", Modifier.weight(1f)) { engine.onEditLeft() }
        EditKey("▶", Modifier.weight(1f)) { engine.onEditRight() }
        EditKey("⌫ word", Modifier.weight(1f)) { engine.onEditWordBack() }
        EditKey("⌫", Modifier.weight(1f)) { engine.onEditBack() }
    }
}

@Composable
private fun EditKey(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalVaaniColors.current
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.key)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(label, color = colors.keyInk)
    }
}

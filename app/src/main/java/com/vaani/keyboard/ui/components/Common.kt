package com.vaani.keyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaani.keyboard.ui.theme.LocalVaaniColors

@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = LocalVaaniColors.current
    Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.ink)
            }
            Text(title, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        }
        subtitle?.let {
            Text(it, color = colors.sub, modifier = Modifier.padding(start = 48.dp, bottom = 10.dp))
        }
        content()
    }
}

@Composable
fun Card(content: @Composable () -> Unit) {
    val colors = LocalVaaniColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(colors.card)
            .padding(12.dp),
    ) {
        content()
    }
}

@Composable
fun ToggleRow(label: String, sub: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalVaaniColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = colors.ink)
            sub?.let { Text(it, color = colors.sub) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SegmentedControl(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        options.forEach { (value, label) ->
            val isOn = value == selected
            if (isOn) {
                Button(onClick = { onSelect(value) }, modifier = Modifier.weight(1f)) { Text(label) }
            } else {
                OutlinedButton(onClick = { onSelect(value) }, modifier = Modifier.weight(1f)) { Text(label) }
            }
        }
    }
}

@Composable
fun ListRow(title: String, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    val colors = LocalVaaniColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(colors.card)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.ink, fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, color = colors.sub) }
        }
        trailing()
    }
}

@Composable
fun EmptyState(emoji: String, message: String) {
    val colors = LocalVaaniColors.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 30.sp)
        Text(message, color = colors.sub)
    }
}

package com.vaani.keyboard.ime.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.vaani.keyboard.data.model.MicMode
import com.vaani.keyboard.ime.KeyboardEngine
import com.vaani.keyboard.ime.KeyboardPanel
import com.vaani.keyboard.ui.theme.LocalVaaniColors

@Composable
fun VoicePanel(engine: KeyboardEngine) {
    val colors = LocalVaaniColors
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .padding(10.dp),
        ) {
            val shown = if (engine.partialTranscript.isNotEmpty()) engine.partialTranscript else engine.transcript
            Text(
                text = if (shown.isEmpty()) "Hold the mic and speak. Nothing leaves this phone." else shown,
                color = if (shown.isEmpty()) colors.sub else colors.ink,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        ) {
            OutlinedButton(onClick = { engine.switchPanel(KeyboardPanel.QWERTY) }) { Text("ABC ⌨") }
            MicButton(engine)
            OutlinedButton(onClick = { engine.onUndo() }) { Text("↶ Undo") }
        }

        Text(engine.statusLine, color = colors.sub, modifier = Modifier.padding(top = 4.dp))

        EditRow(engine)
    }
}

@Composable
private fun MicButton(engine: KeyboardEngine) {
    val colors = LocalVaaniColors
    val background = if (engine.isListening) colors.danger else colors.accent
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(background)
            .pointerInput(engine) {
                detectTapGestures(
                    onPress = {
                        // Compose's own pointerInput press/release maps directly onto the
                        // prototype's pointerdown/pointerup pair for the "hold to talk" mode;
                        // "tap to start/stop" is handled by onTap below instead.
                        if (engine.micMode() == MicMode.HOLD) {
                            engine.startListening()
                            // Whether this resolves true (pointer released on the button, like
                            // the prototype's pointerup) or false (pointer left/cancelled, like
                            // its pointerleave), both stop listening the same way.
                            tryAwaitRelease()
                            engine.stopListening()
                        }
                    },
                    onTap = {
                        if (engine.micMode() == MicMode.TAP) {
                            if (engine.isListening) engine.stopListening() else engine.startListening()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(if (engine.isListening) "⏺" else "🎙", color = colors.accentInk)
    }
}

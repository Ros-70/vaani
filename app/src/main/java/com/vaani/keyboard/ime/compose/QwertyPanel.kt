package com.vaani.keyboard.ime.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.vaani.keyboard.data.model.KeyboardLanguage
import com.vaani.keyboard.ime.KeyLayer
import com.vaani.keyboard.ime.KeyboardEngine
import com.vaani.keyboard.ime.KeyboardLayouts
import com.vaani.keyboard.ui.theme.LocalVaaniColors
import com.vaani.keyboard.ui.theme.VaaniColorPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Which key (if any) currently has its long-press popup open, and that popup's on-screen bounds. */
private class LongPressPopupState {
    var anchorKey: String? by mutableStateOf(null)
    var charBounds: Map<String, Rect> = emptyMap()

    fun pickAt(position: Offset): String? = charBounds.entries.firstOrNull { it.value.contains(position) }?.key
}

@Composable
fun QwertyPanel(engine: KeyboardEngine) {
    val popupState = remember { LongPressPopupState() }
    Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column {
            val rows = KeyboardLayouts.rowsFor(engine.layer)
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEach { key ->
                        val keyWeight = if (key == "shift" || key == "back" || key == "more" || key == "sym") 1.5f else 1f
                        KeyButton(key, engine, popupState, Modifier.weight(keyWeight))
                    }
                }
            }
            BottomRow(engine, popupState)
        }
        popupState.anchorKey?.let { key -> LongPressPopup(key, popupState) }
    }
}

@Composable
private fun BottomRow(engine: KeyboardEngine, popupState: LongPressPopupState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        val layerKey = if (engine.layer == KeyLayer.LETTERS) "sym" else "abc"
        KeyButton(layerKey, engine, popupState, Modifier.weight(1.5f), label = if (layerKey == "sym") "?123" else "ABC")
        KeyButton(",", engine, popupState, Modifier.weight(1f))
        KeyButton("mic", engine, popupState, Modifier.weight(1f), label = "🎙")
        KeyButton(" ", engine, popupState, Modifier.weight(4f), label = languageName(engine), spaceBar = true)
        KeyButton(".", engine, popupState, Modifier.weight(1f))
        KeyButton("enter", engine, popupState, Modifier.weight(1.5f), label = "⏎")
    }
}

private fun languageName(engine: KeyboardEngine): String = when (engine.language) {
    KeyboardLanguage.ENGLISH -> "English"
    KeyboardLanguage.HINDI -> "Hindi"
    KeyboardLanguage.HINGLISH -> "Hinglish"
}

@Composable
private fun KeyButton(
    key: String,
    engine: KeyboardEngine,
    popupState: LongPressPopupState,
    modifier: Modifier,
    label: String = displayLabel(key, engine),
    spaceBar: Boolean = false,
) {
    val colors = LocalVaaniColors
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (pressed) colors.keyAlt else keyBackground(key, colors))
            .pointerInput(key, engine.layer) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pressed = true
                    var resolvedLongPress = false
                    var repeatJob: Job? = null
                    var cursorAnchorX = down.position.x

                    val longPressJob = scope.launch {
                        delay(KeyboardLayouts.LONGPRESS_MS)
                        resolvedLongPress = true
                        when {
                            key == "back" -> {
                                engine.onEditBack()
                                repeatJob = scope.launch {
                                    while (isActive) {
                                        delay(KeyboardLayouts.BACKSPACE_REPEAT_MS)
                                        engine.onEditBack()
                                    }
                                }
                            }
                            spaceBar -> { /* cursor-drag armed; handled in the move loop below */ }
                            KeyboardLayouts.longPressChars[key] != null &&
                                (engine.layer == KeyLayer.LETTERS || key == ".") -> {
                                popupState.anchorKey = key
                            }
                        }
                    }

                    var change: PointerInputChange = down
                    while (true) {
                        val event = awaitPointerEvent()
                        val next = event.changes.firstOrNull { it.id == down.id } ?: break
                        change = next
                        if (resolvedLongPress && spaceBar) {
                            val dx = change.position.x - cursorAnchorX
                            val steps = (dx / KeyboardLayouts.CURSOR_STEP_PX).toInt()
                            if (steps != 0) {
                                engine.moveCaretBy(steps)
                                cursorAnchorX += steps * KeyboardLayouts.CURSOR_STEP_PX
                            }
                        }
                        if (!next.pressed) break
                    }

                    pressed = false
                    longPressJob.cancel()
                    repeatJob?.cancel()

                    when {
                        !resolvedLongPress -> engine.onKeyTap(key)
                        key == "back" -> Unit // already deleted via the repeat job
                        spaceBar -> Unit // caret already moved during the drag
                        popupState.anchorKey == key -> {
                            val pickedChar = popupState.pickAt(change.position)
                            if (pickedChar != null) {
                                engine.onLongPressPick(pickedChar)
                            } else {
                                // Released on the base key itself, not a popup choice -> base char,
                                // Gboard-style: a hold that runs slightly too long is still forgiving.
                                engine.insertChar(key)
                            }
                            popupState.anchorKey = null
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = keyTextColor(key, colors))
    }
}

private fun displayLabel(key: String, engine: KeyboardEngine): String = when (key) {
    "shift" -> "⇧"
    "back" -> "⌫"
    "more" -> "≈<"
    "sym" -> "?123"
    "abc" -> "ABC"
    else -> if (engine.layer == KeyLayer.LETTERS && engine.shiftOn) key.uppercase() else key
}

private fun keyBackground(key: String, colors: VaaniColorPalette) = when (key) {
    "shift", "back", "more", "sym", "abc" -> colors.keyAlt
    "mic", "enter" -> colors.accent
    else -> colors.key
}

private fun keyTextColor(key: String, colors: VaaniColorPalette) = when (key) {
    "mic", "enter" -> colors.accentInk
    else -> colors.keyInk
}

@Composable
private fun LongPressPopup(key: String, popupState: LongPressPopupState) {
    val colors = LocalVaaniColors
    val chars = KeyboardLayouts.longPressChars[key] ?: return
    var bounds by remember { mutableStateOf(emptyMap<String, Rect>()) }
    Row(
        modifier = Modifier
            .zIndex(10f)
            .offset(y = (-54).dp)
            .clip(RoundedCornerShape(9.dp))
            .background(colors.keyAlt)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        chars.forEach { c ->
            Box(
                modifier = Modifier
                    .background(colors.key, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        bounds = bounds + (c to Rect(pos, coords.size.toSize()))
                    },
            ) {
                Text(c, color = colors.keyInk)
            }
        }
    }
    popupState.charBounds = bounds
}

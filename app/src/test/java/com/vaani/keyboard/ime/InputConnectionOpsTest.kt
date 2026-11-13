package com.vaani.keyboard.ime

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Per the build brief's test requirement #2: mocks the real `InputConnection`
 * interface and verifies `commitText`/`deleteSurroundingText`/`sendKeyEvent`
 * are invoked with the right arguments — no live host app or emulator
 * needed for this layer.
 */
@RunWith(RobolectricTestRunner::class)
class InputConnectionOpsTest {

    private lateinit var ic: InputConnection
    private lateinit var ops: InputConnectionOps

    @Before
    fun setUp() {
        ic = mock()
        ops = InputConnectionOps { ic }
    }

    @Test
    fun `commitText calls ic commitText with the given text`() {
        ops.commitText("h")
        verify(ic).commitText("h", 1)
    }

    @Test
    fun `deleteBack deletes exactly one character before the cursor`() {
        ops.deleteBack()
        verify(ic).deleteSurroundingText(1, 0)
    }

    @Test
    fun `deleteCharsBack deletes the given count`() {
        ops.deleteCharsBack(5)
        verify(ic).deleteSurroundingText(5, 0)
    }

    @Test
    fun `deleteCharsBack with zero does nothing`() {
        ops.deleteCharsBack(0)
        verify(ic, org.mockito.kotlin.never()).deleteSurroundingText(any(), any())
    }

    @Test
    fun `moveCaret negative sends DPAD_LEFT down and up events`() {
        ops.moveCaret(-1)
        val captor = argumentCaptor<KeyEvent>()
        verify(ic, org.mockito.kotlin.times(2)).sendKeyEvent(captor.capture())
        assertEquals(KeyEvent.ACTION_DOWN, captor.firstValue.action)
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, captor.firstValue.keyCode)
        assertEquals(KeyEvent.ACTION_UP, captor.secondValue.action)
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, captor.secondValue.keyCode)
    }

    @Test
    fun `moveCaret positive sends DPAD_RIGHT events the given number of times`() {
        ops.moveCaret(3)
        verify(ic, org.mockito.kotlin.times(6)).sendKeyEvent(any()) // 3 steps * (down + up)
    }

    @Test
    fun `moveCaret zero sends no key events`() {
        ops.moveCaret(0)
        verify(ic, org.mockito.kotlin.never()).sendKeyEvent(any())
    }

    @Test
    fun `sendEnter sends KEYCODE_ENTER down and up, not a literal newline`() {
        ops.sendEnter()
        val captor = argumentCaptor<KeyEvent>()
        verify(ic, org.mockito.kotlin.times(2)).sendKeyEvent(captor.capture())
        assertEquals(KeyEvent.KEYCODE_ENTER, captor.firstValue.keyCode)
        assertEquals(KeyEvent.KEYCODE_ENTER, captor.secondValue.keyCode)
        verify(ic, org.mockito.kotlin.never()).commitText(org.mockito.kotlin.eq("\n"), any())
    }

    @Test
    fun `currentWord derives fresh from getTextBeforeCursor, Latin and Devanagari`() {
        whenever(ic.getTextBeforeCursor(40, 0)).thenReturn("hello world")
        assertEquals("world", ops.currentWord())

        whenever(ic.getTextBeforeCursor(40, 0)).thenReturn("कल सुबह")
        assertEquals("सुबह", ops.currentWord())
    }

    @Test
    fun `currentWord returns empty string when the cursor is after whitespace`() {
        whenever(ic.getTextBeforeCursor(40, 0)).thenReturn("hello ")
        assertEquals("", ops.currentWord())
    }

    @Test
    fun `replaceCurrentWordWith deletes the current word length then commits the pick plus a space`() {
        whenever(ic.getTextBeforeCursor(40, 0)).thenReturn("kal")
        ops.replaceCurrentWordWith("कल")
        verify(ic).deleteSurroundingText(3, 0)
        verify(ic).commitText("कल ", 1)
    }

    @Test
    fun `deleteWordBack removes the trailing word and its leading whitespace`() {
        whenever(ic.getTextBeforeCursor(100, 0)).thenReturn("hello there world")
        ops.deleteWordBack()
        // "\s*\S+\s*$" over "hello there world" matches " world" (6 chars).
        verify(ic).deleteSurroundingText(6, 0)
    }
}

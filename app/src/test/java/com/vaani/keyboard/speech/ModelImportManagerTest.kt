package com.vaani.keyboard.speech

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A real "Whisper small" download was rejected by the app with "That
 * doesn't look like a speech model file whisper.cpp can read" — the
 * previous version of [looksLikeGgmlOrGguf] compared a file's header
 * against the ASCII *spelling* of ggml's magic constant names ("ggml",
 * "ggjt") instead of their actual on-disk little-endian byte order, so it
 * rejected every real ggml model file. These tests use the real byte
 * sequences a `ggml-*.bin`/`.gguf` file actually starts with.
 */
class ModelImportManagerTest {

    @Test
    fun `accepts a real classic ggml header (little-endian 0x67676d6c)`() {
        assertTrue(looksLikeGgmlOrGguf(byteArrayOf(0x6c, 0x6d, 0x67, 0x67)))
    }

    @Test
    fun `accepts a real ggjt-quantized ggml header (little-endian 0x67676a74)`() {
        assertTrue(looksLikeGgmlOrGguf(byteArrayOf(0x74, 0x6a, 0x67, 0x67)))
    }

    @Test
    fun `accepts a real GGUF header (literal ASCII bytes)`() {
        assertTrue(looksLikeGgmlOrGguf("GGUF".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun `rejects the literal ASCII spelling of ggml's define name, which is not what's on disk`() {
        assertFalse(looksLikeGgmlOrGguf("ggml".toByteArray(Charsets.US_ASCII)))
        assertFalse(looksLikeGgmlOrGguf("ggjt".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun `rejects an unrelated file header`() {
        assertFalse(looksLikeGgmlOrGguf(byteArrayOf(0x50, 0x4b, 0x03, 0x04))) // a zip file's magic, for contrast
    }
}

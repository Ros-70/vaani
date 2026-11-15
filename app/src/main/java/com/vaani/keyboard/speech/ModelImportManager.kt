package com.vaani.keyboard.speech

import android.content.Context
import android.net.Uri
import java.io.File

sealed class ModelImportResult {
    data class Success(val fileName: String) : ModelImportResult()
    data class Rejected(val reason: String) : ModelImportResult()
}

/**
 * True if the first 4 bytes of a file look like a GGUF or classic
 * ggml/ggml-quantized ("ggjt") model header. Pulled out as a free function
 * (no `Context`/Android dependency) so it's directly unit-testable — see
 * `ModelImportManagerTest`, which pins down the exact on-disk byte order
 * after an earlier version of this check used the wrong one and rejected
 * every real whisper.cpp model file.
 *
 * GGUF files start with the literal ASCII bytes "GGUF", in that order.
 * Classic ggml files are different: their magic is a 32-bit integer
 * (0x67676d6c for plain ggml, 0x67676a74 for the "ggjt" quantized variant)
 * written LITTLE-ENDIAN — so on disk the bytes are reversed from how the
 * define's name reads, e.g. ggml's on-disk bytes spell "lmgg", not "ggml".
 */
fun looksLikeGgmlOrGguf(header: ByteArray): Boolean {
    val asGguf = header.contentEquals(byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte()))
    val asGgml = header.contentEquals(byteArrayOf(0x6c, 0x6d, 0x67, 0x67)) // little-endian bytes of 0x67676d6c
    val asGgjt = header.contentEquals(byteArrayOf(0x74, 0x6a, 0x67, 0x67)) // little-endian bytes of 0x67676a74
    return asGguf || asGgml || asGgjt
}

/**
 * Copies a model file picked via Storage Access Framework
 * (`ACTION_OPEN_DOCUMENT`) into app-private storage, validating it looks
 * like a GGML/GGUF model whisper.cpp can load before accepting it — never a
 * broad storage permission, just the one file R picked.
 */
class ModelImportManager(private val context: Context) {

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    fun import(uri: Uri, suggestedFileName: String): ModelImportResult {
        val resolver = context.contentResolver
        val input = resolver.openInputStream(uri) ?: return ModelImportResult.Rejected("Couldn't open that file.")

        val header = ByteArray(4)
        val tempFile = File(modelsDir, "$suggestedFileName.part")

        input.use { stream ->
            val buffered = stream.buffered()
            val read = buffered.read(header)
            if (read < 4 || !looksLikeGgmlOrGguf(header)) {
                return ModelImportResult.Rejected(
                    "That doesn't look like a speech model file whisper.cpp can read.",
                )
            }
            tempFile.outputStream().use { out ->
                out.write(header, 0, read)
                buffered.copyTo(out)
            }
        }

        val finalFile = File(modelsDir, suggestedFileName)
        if (finalFile.exists()) finalFile.delete()
        tempFile.renameTo(finalFile)
        return ModelImportResult.Success(finalFile.name)
    }
}

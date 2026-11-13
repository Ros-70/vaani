package com.vaani.keyboard.speech

import android.content.Context
import android.net.Uri
import java.io.File

sealed class ModelImportResult {
    data class Success(val fileName: String) : ModelImportResult()
    data class Rejected(val reason: String) : ModelImportResult()
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

    private fun looksLikeGgmlOrGguf(header: ByteArray): Boolean {
        // GGML magic (0x67676d6c, "ggml" little-endian variants used across versions)
        // and GGUF magic ("GGUF" ASCII) — whisper.cpp model files use one of these.
        val asGguf = header.contentEquals(byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte()))
        val asGgmlAscii = header.contentEquals(byteArrayOf('g'.code.toByte(), 'g'.code.toByte(), 'm'.code.toByte(), 'l'.code.toByte()))
        val asGgjt = header.contentEquals(byteArrayOf('g'.code.toByte(), 'g'.code.toByte(), 'j'.code.toByte(), 't'.code.toByte()))
        return asGguf || asGgmlAscii || asGgjt
    }
}

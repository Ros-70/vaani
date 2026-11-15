package com.vaani.keyboard.speech

import android.content.Context
import android.util.Log
import com.vaani.keyboard.util.DebugLog

/**
 * JNI bridge to whisper.cpp (see app/src/main/cpp). This is the engine the
 * build brief recommends: the most proven, actively maintained, genuinely
 * offline runtime with existing Android build tooling, over a hand-rolled
 * ONNX Runtime Mobile integration.
 *
 * NOTE for whoever picks this up next (see HANDOVER.md): the sandbox this
 * was written in has no Android SDK/NDK and a network policy that only
 * allows dl.google.com, so this class and app/src/main/cpp/jni_bridge.cpp
 * were written against whisper.cpp's public C API from documentation/memory
 * and could not be compiled or run here. CI (which has full internet access
 * and a real NDK) is the first real build — verify there before shipping.
 */
class WhisperEngine(context: Context) : SpeechEngine {

    private val appContext = context.applicationContext
    private var nativeHandle: Long = 0L
    override var isLoaded: Boolean = false
        private set
    override var isStreaming: Boolean = false
        private set

    override fun loadModel(modelFilePath: String, streaming: Boolean): Boolean {
        unload()
        logI("loadModel(): loading $modelFilePath (streaming=$streaming)")
        return try {
            nativeHandle = nativeLoad(modelFilePath)
            isLoaded = nativeHandle != 0L
            isStreaming = streaming && isLoaded
            logI("loadModel(): nativeHandle=$nativeHandle isLoaded=$isLoaded")
            isLoaded
        } catch (t: Throwable) {
            logE("Failed to load model $modelFilePath", t)
            isLoaded = false
            false
        }
    }

    override fun unload() {
        if (nativeHandle != 0L) {
            runCatching { nativeUnload(nativeHandle) }
            nativeHandle = 0L
        }
        isLoaded = false
        isStreaming = false
    }

    override fun transcribe(pcm16Mono16k: ShortArray): String {
        if (!isLoaded) {
            logI("transcribe() called but isLoaded=false — no model is actually loaded")
            return ""
        }
        if (pcm16Mono16k.isEmpty()) {
            logI("transcribe() called with an empty buffer — nothing was recorded")
            return ""
        }
        val seconds = pcm16Mono16k.size / 16_000.0
        logI("transcribe(): calling nativeTranscribeFull on ${pcm16Mono16k.size} samples (~${"%.2f".format(seconds)}s), nativeHandle=$nativeHandle")
        val startMs = System.currentTimeMillis()
        val result = runCatching { nativeTranscribeFull(nativeHandle, pcm16Mono16k) }
            .onFailure { logE("nativeTranscribeFull threw", it) }
            .getOrDefault("")
        logI("transcribe(): nativeTranscribeFull returned after ${System.currentTimeMillis() - startMs}ms, result=\"$result\"")
        return result
    }

    override fun transcribePartial(pcm16Mono16k: ShortArray): String {
        if (!isLoaded) return ""
        return runCatching { nativeTranscribeChunk(nativeHandle, pcm16Mono16k) }
            .onFailure { logE("nativeTranscribeChunk threw", it) }
            .getOrDefault("")
    }

    override fun finalizeStream(): String {
        if (!isLoaded) return ""
        logI("finalizeStream(): calling nativeFinalizeStream, nativeHandle=$nativeHandle")
        val startMs = System.currentTimeMillis()
        val result = runCatching { nativeFinalizeStream(nativeHandle) }
            .onFailure { logE("nativeFinalizeStream threw", it) }
            .getOrDefault("")
        logI("finalizeStream(): returned after ${System.currentTimeMillis() - startMs}ms, result=\"$result\"")
        return result
    }

    override fun resetStream() {
        if (isLoaded) runCatching { nativeResetStream(nativeHandle) }
    }

    private fun logI(message: String) {
        Log.i(TAG, message)
        DebugLog.log(TAG, message)
    }

    private fun logE(message: String, t: Throwable) {
        Log.e(TAG, message, t)
        DebugLog.log(TAG, "$message — ${t::class.simpleName}: ${t.message}")
    }

    private external fun nativeLoad(modelPath: String): Long
    private external fun nativeUnload(handle: Long)
    private external fun nativeTranscribeFull(handle: Long, pcm: ShortArray): String
    private external fun nativeTranscribeChunk(handle: Long, pcm: ShortArray): String
    private external fun nativeFinalizeStream(handle: Long): String
    private external fun nativeResetStream(handle: Long)

    companion object {
        private const val TAG = "WhisperEngine"
        init {
            runCatching { System.loadLibrary("vaani_whisper") }
                .onFailure { Log.e(TAG, "Native library vaani_whisper not available", it) }
        }
    }
}

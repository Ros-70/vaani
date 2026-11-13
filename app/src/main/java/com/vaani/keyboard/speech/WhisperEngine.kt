package com.vaani.keyboard.speech

import android.content.Context
import android.util.Log

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
        return try {
            nativeHandle = nativeLoad(modelFilePath)
            isLoaded = nativeHandle != 0L
            isStreaming = streaming && isLoaded
            isLoaded
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load model $modelFilePath", t)
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
        if (!isLoaded || pcm16Mono16k.isEmpty()) return ""
        return runCatching { nativeTranscribeFull(nativeHandle, pcm16Mono16k) }.getOrDefault("")
    }

    override fun transcribePartial(pcm16Mono16k: ShortArray): String {
        if (!isLoaded) return ""
        return runCatching { nativeTranscribeChunk(nativeHandle, pcm16Mono16k) }.getOrDefault("")
    }

    override fun finalizeStream(): String {
        if (!isLoaded) return ""
        return runCatching { nativeFinalizeStream(nativeHandle) }.getOrDefault("")
    }

    override fun resetStream() {
        if (isLoaded) runCatching { nativeResetStream(nativeHandle) }
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

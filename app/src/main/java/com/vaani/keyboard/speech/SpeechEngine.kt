package com.vaani.keyboard.speech

/**
 * Abstraction over the on-device speech-to-text runtime so the IME and its
 * tests don't depend on the concrete whisper.cpp JNI binding (see
 * [WhisperEngine]). Audio is always mono 16-bit PCM at 16kHz — Whisper's
 * expected sample rate — regardless of which model is loaded.
 */
interface SpeechEngine {
    val isLoaded: Boolean
    val isStreaming: Boolean

    /** Loads a sideloaded model file. Returns false if the file isn't a model this engine can read. */
    fun loadModel(modelFilePath: String, streaming: Boolean): Boolean

    fun unload()

    /** One-shot path (non-streaming models): the full recording, decoded in one call. */
    fun transcribe(pcm16Mono16k: ShortArray): String

    /** Streaming path: call with each new chunk; returns the best current partial transcript. */
    fun transcribePartial(pcm16Mono16k: ShortArray): String

    /** Call once a streaming session's audio is complete, to flush/finalize the transcript. */
    fun finalizeStream(): String

    fun resetStream()
}

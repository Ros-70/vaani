package com.vaani.keyboard.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

/**
 * Mono 16kHz 16-bit PCM capture via [AudioRecord] — Whisper's expected
 * sample rate. Streams chunks for streaming-capable models and always
 * accumulates the full buffer for the one-shot (non-streaming) path.
 * Silence-stop is a simple RMS-energy-over-a-rolling-window check, no VAD
 * model needed, per the build brief.
 */
class AudioCapture(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val SILENCE_RMS_THRESHOLD = 400.0 // empirical threshold for 16-bit PCM silence
        private const val SILENCE_WINDOW_MS = 1500L
        private const val CHUNK_MS = 320L
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var recording = false
    private val fullBuffer = mutableListOf<Short>()

    fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /**
     * Returns false (and starts nothing) if `RECORD_AUDIO` isn't granted —
     * an IME service can't itself show the system permission dialog (only
     * an Activity can), so the caller is responsible for routing to the
     * companion app's request flow first. See
     * `KeyboardEngine.startListening`/`VaaniInputMethodService`.
     */
    fun start(
        streaming: Boolean,
        silenceStopEnabled: Boolean,
        onChunk: (ShortArray) -> Unit,
        onSilenceDetected: () -> Unit,
    ): Boolean {
        if (recording) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        fullBuffer.clear()

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(minBufferSize, SAMPLE_RATE / 1000 * 2 * (CHUNK_MS.toInt()))

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        audioRecord = record
        recording = true
        record.startRecording()

        val chunkSamples = (SAMPLE_RATE * CHUNK_MS / 1000L).toInt()
        var silentForMs = 0L

        recordingThread = Thread {
            val chunk = ShortArray(chunkSamples)
            while (recording) {
                val read = record.read(chunk, 0, chunk.size)
                if (read <= 0) continue
                val samples = if (read == chunk.size) chunk else chunk.copyOf(read)

                synchronized(fullBuffer) {
                    fullBuffer.addAll(samples.toList())
                }

                if (streaming) onChunk(samples)

                if (silenceStopEnabled) {
                    val rms = rms(samples)
                    silentForMs = if (rms < SILENCE_RMS_THRESHOLD) silentForMs + CHUNK_MS else 0L
                    if (silentForMs >= SILENCE_WINDOW_MS) {
                        onSilenceDetected()
                        break
                    }
                }
            }
        }.also { it.start() }
        return true
    }

    /** Stops capture and returns the full held-duration recording, for the one-shot path. */
    fun stopAndGetBuffer(): ShortArray {
        recording = false
        recordingThread?.join(500)
        recordingThread = null
        audioRecord?.apply {
            runCatching { stop() }
            release()
        }
        audioRecord = null
        return synchronized(fullBuffer) { fullBuffer.toShortArray() }
    }

    private fun rms(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var sumSquares = 0.0
        for (s in samples) sumSquares += (s.toDouble() * s.toDouble())
        return sqrt(sumSquares / samples.size)
    }
}

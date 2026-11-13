package com.vaani.keyboard.ime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaani.keyboard.data.VaaniRepository
import com.vaani.keyboard.data.model.InsertMode
import com.vaani.keyboard.data.model.KeyboardLanguage
import com.vaani.keyboard.data.model.ModelInfo
import com.vaani.keyboard.data.model.PerAppMode
import com.vaani.keyboard.data.model.Settings
import com.vaani.keyboard.data.model.Snippet
import com.vaani.keyboard.data.model.VocabEntry
import com.vaani.keyboard.speech.AudioCapture
import com.vaani.keyboard.speech.SpeechEngine
import com.vaani.textpipeline.PipelineSettings
import com.vaani.textpipeline.VocabEntry as PipelineVocabEntry
import com.vaani.textpipeline.Snippet as PipelineSnippet
import com.vaani.textpipeline.postProcess
import com.vaani.textpipeline.trySnippetCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class EditorContext(
    val packageName: String,
    val isPasswordField: Boolean,
)

/**
 * Holds all Compose UI state for the keyboard surface and drives it: the
 * tap/hold race, dictation, text post-processing, and every
 * [InputConnectionOps] call. One instance is created per IME input session
 * (see VaaniInputMethodService); it reads live settings/vocab/snippets from
 * [repository] rather than caching them, so edits made in the companion app
 * take effect immediately.
 */
class KeyboardEngine(
    private val repository: VaaniRepository,
    private val speechEngine: SpeechEngine,
    private val audioCapture: AudioCapture,
    private val ops: InputConnectionOps,
    private val scope: CoroutineScope,
    private val onOpenCompanion: (screen: String) -> Unit,
) {
    var panel by mutableStateOf(KeyboardPanel.VOICE)
    var layer by mutableStateOf(KeyLayer.LETTERS)
    var shiftOn by mutableStateOf(false)
    var language by mutableStateOf(KeyboardLanguage.ENGLISH)

    var isListening by mutableStateOf(false)
    var transcript by mutableStateOf("")
    var partialTranscript by mutableStateOf("")
    var statusLine by mutableStateOf("")
    var firedChips by mutableStateOf<List<String>>(emptyList())

    var reviewBarOpen by mutableStateOf(false)
    var reviewText by mutableStateOf("")

    var suggestionWord by mutableStateOf("")
    var suggestionCandidates by mutableStateOf<List<String>>(emptyList())

    var editorContext by mutableStateOf(EditorContext(packageName = "", isPasswordField = false))

    private var settingsSnapshot: Settings = Settings()
    private var vocabSnapshot: List<VocabEntry> = emptyList()
    private var snippetsSnapshot: List<Snippet> = emptyList()
    private var modelsSnapshot: List<ModelInfo> = emptyList()
    private var lastInsertion: Pair<String, String>? = null // Not text, but used for the Undo affordance signal.
    private var activeModel: ModelInfo? = null

    private var collectJob: Job? = null

    fun start() {
        collectJob = scope.launch {
            repository.settings.onEach { settingsSnapshot = it }.launchIn(scope)
            repository.vocab.onEach { vocabSnapshot = it }.launchIn(scope)
            repository.snippets.onEach { snippetsSnapshot = it }.launchIn(scope)
            repository.models.onEach { models ->
                modelsSnapshot = models
                activeModel = models.firstOrNull { it.id == settingsSnapshot.activeModelId }
            }.launchIn(scope)
        }
    }

    fun stop() {
        collectJob?.cancel()
        if (isListening) stopListening()
    }

    fun micMode() = settingsSnapshot.mic

    private fun effectiveInsertMode(): InsertMode {
        val per = settingsSnapshot.perApp[editorContext.packageName] ?: PerAppMode.DEFAULT
        return when (per) {
            PerAppMode.DEFAULT -> settingsSnapshot.insertMode
            PerAppMode.DIRECT -> InsertMode.DIRECT
            PerAppMode.REVIEW -> InsertMode.REVIEW
        }
    }

    // ---------------- Panel / layer switching ----------------

    fun switchPanel(target: KeyboardPanel) {
        panel = target
        if (target == KeyboardPanel.QWERTY) refreshSuggestionStrip()
    }

    fun switchLayer(target: KeyLayer) {
        layer = target
        if (target == KeyLayer.LETTERS) shiftOn = false
    }

    fun cycleLanguage() {
        language = when (language) {
            KeyboardLanguage.ENGLISH -> KeyboardLanguage.HINDI
            KeyboardLanguage.HINDI -> KeyboardLanguage.HINGLISH
            KeyboardLanguage.HINGLISH -> KeyboardLanguage.ENGLISH
        }
        refreshSuggestionStrip()
    }

    fun toggleShift() {
        shiftOn = !shiftOn
    }

    // ---------------- QWERTY key handling ----------------

    fun onKeyTap(key: String) {
        when (key) {
            "shift" -> toggleShift()
            "sym" -> switchLayer(KeyLayer.SYMBOLS)
            "more" -> switchLayer(KeyLayer.MORE)
            "abc" -> switchLayer(KeyLayer.LETTERS)
            "mic" -> switchPanel(KeyboardPanel.VOICE)
            "back" -> { ops.deleteBack(); refreshSuggestionStrip() }
            "enter" -> ops.sendEnter()
            else -> insertChar(key)
        }
    }

    fun insertChar(raw: String) {
        var text = raw
        if (layer == KeyLayer.LETTERS && shiftOn) {
            text = raw.uppercase()
            shiftOn = false
        }
        ops.commitText(text)
        refreshSuggestionStrip()
    }

    fun onLongPressPick(char: String) {
        ops.commitText(char)
        refreshSuggestionStrip()
    }

    // ---------------- Edit row ----------------

    fun onEditLeft() = ops.moveCaret(-1)
    fun onEditRight() = ops.moveCaret(1)
    fun moveCaretBy(delta: Int) = ops.moveCaret(delta)
    fun onEditWordBack() { ops.deleteWordBack(); refreshSuggestionStrip() }
    fun onEditBack() { ops.deleteBack(); refreshSuggestionStrip() }

    // ---------------- Suggestion strip / transliteration ----------------

    private fun refreshSuggestionStrip() {
        if (editorContext.isPasswordField) {
            suggestionWord = ""
            suggestionCandidates = emptyList()
            return
        }
        val word = ops.currentWord()
        suggestionWord = word
        suggestionCandidates = if (language != KeyboardLanguage.ENGLISH && word.isNotEmpty()) {
            KeyboardLayouts.translit[word.lowercase()] ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun onSuggestionPick(pick: String) {
        ops.replaceCurrentWordWith(pick)
        refreshSuggestionStrip()
    }

    // ---------------- Dictation ----------------

    fun startListening() {
        if (isListening) return
        isListening = true
        transcript = ""
        partialTranscript = ""
        statusLine = "Listening… audio stays in memory"
        speechEngine.resetStream()
        audioCapture.start(
            streaming = activeModel?.streaming == true,
            silenceStopEnabled = settingsSnapshot.silenceStop,
            onChunk = { pcm ->
                if (activeModel?.streaming == true) {
                    partialTranscript = speechEngine.transcribePartial(pcm)
                }
            },
            onSilenceDetected = { if (isListening) stopListening() },
        )
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        val pcm = audioCapture.stopAndGetBuffer()
        statusLine = "Transcribing…"

        val raw = if (activeModel?.streaming == true) {
            speechEngine.finalizeStream()
        } else {
            speechEngine.transcribe(pcm)
        }
        if (raw.isBlank()) {
            statusLine = ""
            return
        }
        handleRawTranscript(raw)
    }

    private fun handleRawTranscript(raw: String) {
        val snippetHit = trySnippetCommand(raw, snippetsSnapshot.map { PipelineSnippet(it.trigger, it.text) })
        if (snippetHit != null) {
            transcript = raw
            statusLine = "Snippet matched"
            deliver(snippetHit)
            return
        }

        val pipelineSettings = PipelineSettings(
            autoPunct = settingsSnapshot.autoPunct,
            spokenPunct = settingsSnapshot.spokenPunct,
            autoCaps = settingsSnapshot.autoCaps,
        )
        val pipelineVocab = vocabSnapshot.map { PipelineVocabEntry(it.spoken, it.written) }
        val result = postProcess(raw, pipelineSettings, pipelineVocab)
        transcript = result.text
        firedChips = result.fired
        statusLine = if (result.fired.isNotEmpty()) {
            "Corrected ${result.fired.size} term${if (result.fired.size > 1) "s" else ""}"
        } else {
            "Transcribed on-device"
        }
        deliver(result.text)
    }

    private fun deliver(text: String) {
        if (editorContext.isPasswordField) {
            ops.commitText(text)
            return
        }
        if (effectiveInsertMode() == InsertMode.REVIEW) {
            reviewText = text
            reviewBarOpen = true
        } else {
            insertFinal(text)
        }
    }

    private fun insertFinal(text: String) {
        ops.commitText(text)
        lastInsertion = editorContext.packageName to text
        if (settingsSnapshot.historyEnabled && !editorContext.isPasswordField) {
            scope.launch {
                repository.addHistoryEntry(text, editorContext.packageName, language.name.lowercase())
            }
        }
    }

    fun onReviewInsert() {
        reviewBarOpen = false
        val t = reviewText.trim()
        if (t.isNotEmpty()) insertFinal(t)
    }

    fun onReviewDiscard() {
        reviewBarOpen = false
    }

    fun onReviewRedictate() {
        reviewBarOpen = false
        startListening()
    }

    fun onUndo() {
        val (_, text) = lastInsertion ?: return
        // Real equivalent of the prototype's "restore field to its pre-insertion snapshot":
        // delete exactly as many characters as we just committed. Correct as long as the
        // caret is still right after that insertion, which holds immediately after dictation
        // delivers — the only time Undo is meaningfully pressed.
        ops.deleteCharsBack(text.length)
        lastInsertion = null
    }

    // ---------------- Companion app launches ----------------

    fun openHistory() = onOpenCompanion("history")
    fun openSnippets() = onOpenCompanion("snippets")
    fun openSettings() = onOpenCompanion("settings")

    fun insertSnippetDirectly(snippet: Snippet) {
        deliver(snippet.text)
    }

    suspend fun currentActiveModel(): ModelInfo? {
        val models = repository.models.first()
        return models.firstOrNull { it.id == settingsSnapshot.activeModelId }
    }
}

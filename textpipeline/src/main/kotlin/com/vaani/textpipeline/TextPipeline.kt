package com.vaani.textpipeline

/**
 * Pure, Android-free port of the post-processing pipeline from
 * vaani-keyboard-v1_0.html. No InputConnection, no Context — these are the
 * functions with zero host-app dependency, callable from the IME, from a
 * plain JUnit test, or anywhere else.
 */

data class VocabEntry(val spoken: String, val written: String)

data class Snippet(val trigger: String, val text: String)

data class PipelineSettings(
    val autoPunct: Boolean = true,
    val spokenPunct: Boolean = true,
    val autoCaps: Boolean = true,
)

data class SpokenPunctuationResult(val text: String, val hits: Int)

data class PostProcessResult(val text: String, val fired: List<String>)

private val PUNCT_MAP: List<Pair<String, String>> = listOf(
    "comma" to ",",
    "full stop" to ".",
    "period" to ".",
    "question mark" to "?",
    "exclamation mark" to "!",
    "colon" to ":",
    "semicolon" to ";",
    "new line" to "\n",
    "new paragraph" to "\n\n",
).sortedByDescending { it.first.length } // longest phrase first: "new paragraph" wins over "new line"

private fun phraseRegex(phrase: String): Regex =
    Regex("\\s*\\b" + phrase.replace(" ", "\\s+") + "\\b", RegexOption.IGNORE_CASE)

fun applySpokenPunctuation(text: String, spokenPunctEnabled: Boolean = true): SpokenPunctuationResult {
    if (!spokenPunctEnabled) return SpokenPunctuationResult(text, 0)
    var out = text
    var hits = 0
    for ((phrase, symbol) in PUNCT_MAP) {
        out = phraseRegex(phrase).replace(out) {
            hits++
            symbol
        }
    }
    return SpokenPunctuationResult(out, hits)
}

private fun Regex.Companion.escapeLiteralKeepingSpaces(s: String): String {
    // Same escaping as the prototype's /[.*+?^${}()|[\]\\]/g, then collapse
    // runs of whitespace in the ORIGINAL phrase into \s+ so multi-word
    // phrases match flexible whitespace between words.
    val escaped = s.replace(Regex("[.*+?^\${}()|\\[\\]\\\\]")) { "\\" + it.value }
    return escaped.replace(Regex("\\s+"), "\\\\s+")
}

fun applyVocabulary(text: String, vocab: List<VocabEntry>): PostProcessResult {
    var out = text
    val fired = mutableListOf<String>()
    // Walked in STORED order (not text-occurrence order) — matters for the
    // "fired" chip list and must match exactly.
    for (entry in vocab) {
        if (entry.spoken.isBlank()) continue
        val pattern = "\\b" + Regex.escapeLiteralKeepingSpaces(entry.spoken) + "\\b"
        val re = Regex(pattern, RegexOption.IGNORE_CASE)
        if (re.containsMatchIn(out)) {
            // Kotlin's Regex.replace(String) treats '$' specially (group refs);
            // escape it so a written form containing a literal '$' is inserted as-is.
            out = re.replace(out, entry.written.replace("$", "\${\"\$\"}"))
            fired.add(entry.written)
        }
    }
    return PostProcessResult(out, fired)
}

fun applyAutoCaps(text: String, autoCapsEnabled: Boolean = true): String {
    if (!autoCapsEnabled) return text
    // Sentence-case: first letter of the string, and the first letter after
    // ". ", "! ", "? " or a newline — not every word.
    val re = Regex("(^|[.!?]\\s+|\\n\\s*)([a-z])")
    return re.replace(text) { m ->
        m.groupValues[1] + m.groupValues[2].uppercase()
    }
}

fun tidySpacing(text: String): String {
    var out = text
    out = out.replace(Regex("\\s+([,.;:!?])")) { it.groupValues[1] }
    out = out.replace(Regex("([,;:])(?=\\S)")) { it.groupValues[1] + " " }
    out = out.replace(Regex("\\.(?=[A-Za-z])"), ". ")
    out = out.replace(Regex("[ \\t]{2,}"), " ")
    out = out.replace(Regex(" *\\n *"), "\n")
    return out.trim()
}

fun postProcess(
    raw: String,
    settings: PipelineSettings = PipelineSettings(),
    vocab: List<VocabEntry> = emptyList(),
): PostProcessResult {
    var t = raw
    val sp = applySpokenPunctuation(t, settings.spokenPunct)
    t = sp.text
    val vc = applyVocabulary(t, vocab)
    t = vc.text
    val fired = vc.fired
    if (settings.autoPunct && !Regex("[.!?]\\s*$").containsMatchIn(t)) {
        t = t + "."
    }
    t = tidySpacing(t)
    t = applyAutoCaps(t, settings.autoCaps)
    return PostProcessResult(t, fired)
}

private val SNIPPET_COMMAND = Regex("^insert\\s+(.+)$", RegexOption.IGNORE_CASE)

/** Matches "insert <trigger>"; returns the matched snippet's full text, or null. */
fun trySnippetCommand(raw: String, snippets: List<Snippet>): String? {
    val m = SNIPPET_COMMAND.find(raw.trim()) ?: return null
    val want = m.groupValues[1].trim().trimEnd('.', '!', '?').lowercase()
    return snippets.firstOrNull { it.trigger.lowercase() == want }?.text
}

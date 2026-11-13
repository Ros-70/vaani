package com.vaani.keyboard.ime

/** Port of the prototype's LAYOUTS object — no Android dependency, plain data. */
enum class KeyLayer { LETTERS, SYMBOLS, MORE }

enum class KeyboardPanel { VOICE, QWERTY }

object KeyboardLayouts {
    val letters = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("shift", "z", "x", "c", "v", "b", "n", "m", "back"),
    )

    val symbols = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "₹", "_", "&", "-", "+", "(", ")", "/"),
        listOf("more", "*", "\"", "'", ":", ";", "!", "?", "back"),
    )

    val more = listOf(
        listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"),
        listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\"),
        listOf("sym", "%", "©", "®", "™", "[", "]", "<", ">", "back"),
    )

    fun rowsFor(layer: KeyLayer): List<List<String>> = when (layer) {
        KeyLayer.LETTERS -> letters
        KeyLayer.SYMBOLS -> symbols
        KeyLayer.MORE -> more
    }

    /** Vowels and a few consonants whose long-press shows accented variants, plus "." for suffixes. */
    val longPressChars: Map<String, List<String>> = mapOf(
        "a" to listOf("á", "à", "â", "ã", "ä"),
        "e" to listOf("é", "è", "ê", "ë"),
        "i" to listOf("í", "ì", "î", "ï"),
        "o" to listOf("ó", "ò", "ô", "õ", "ö"),
        "u" to listOf("ú", "ù", "û", "ü"),
        "n" to listOf("ñ"),
        "c" to listOf("ç"),
        "s" to listOf("ś", "š"),
        "y" to listOf("ÿ"),
        "z" to listOf("ž"),
        "." to listOf(".com", ".in", ".org", "..."),
    )

    const val LONGPRESS_MS = 420L
    const val BACKSPACE_REPEAT_MS = 90L
    const val CURSOR_STEP_PX = 14f

    /** Small Latin→Devanagari lookup — an intentionally scope-limited MVP, see HANDOVER.md. */
    val translit: Map<String, List<String>> = mapOf(
        "kal" to listOf("कल", "काल"), "subah" to listOf("सुबह"), "das" to listOf("दस"),
        "baje" to listOf("बजे"), "baithak" to listOf("बैठक", "बैठकें"), "hai" to listOf("है"),
        "namaste" to listOf("नमस्ते"), "report" to listOf("रिपोर्ट"), "taiyar" to listOf("तैयार"),
        "rakhen" to listOf("रखें"), "kripya" to listOf("कृपया"), "theek" to listOf("ठीक"),
        "ji" to listOf("जी"), "aaj" to listOf("आज"), "kaam" to listOf("काम"), "round" to listOf("राउंड"),
    )
}

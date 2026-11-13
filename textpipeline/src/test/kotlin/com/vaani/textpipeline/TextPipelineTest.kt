package com.vaani.textpipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Golden values in this file were produced by running the actual functions
 * from vaani-keyboard-v1_0.html in Node.js (not hand-derived from reading
 * the regexes), per the build brief's instructions. See the brief's two
 * required full-pipeline examples (first two cases below) plus additional
 * fixtures covering applyVocabulary's stored-order firing, tidySpacing's
 * whitespace rules, and applyAutoCaps's multi-sentence behavior.
 */
class TextPipelineTest {

    private val defaultVocab = listOf(
        VocabEntry("gem portal", "GeM portal"),
        VocabEntry("g f r", "GFR"),
        VocabEntry("n quas", "NQAS"),
        VocabEntry("lakshya", "LaQshya"),
        VocabEntry("o p d", "OPD"),
        VocabEntry("b p", "BP"),
        VocabEntry("s p o two", "SpO2"),
        VocabEntry("i c u", "ICU"),
    )

    private val defaultSnippets = listOf(
        Snippet("address", "Room 214, Administrative Block,\nDistrict Hospital, New Delhi — 110001"),
        Snippet("regards", "Kind regards,\nR\nHospital Administration"),
    )

    private fun process(raw: String) = postProcess(raw, PipelineSettings(), defaultVocab)

    @Test
    fun `brief golden value 1 - GeM order`() {
        val r = process(
            "please check the gem portal for the ventilator order comma and share the g f r clause with the finance team full stop"
        )
        assertEquals(
            "Please check the GeM portal for the ventilator order, and share the GFR clause with the finance team.",
            r.text,
        )
        assertEquals(listOf("GeM portal", "GFR"), r.fired)
    }

    @Test
    fun `brief golden value 2 - vitals`() {
        val r = process("patient ka b p is one forty over ninety and s p o two is ninety six percent")
        assertEquals("Patient ka BP is one forty over ninety and SpO2 is ninety six percent.", r.text)
        assertEquals(listOf("BP", "SpO2"), r.fired)
    }

    @Test
    fun `hinglish ward round`() {
        val r = process("kal morning ko o p d ka round hai please ensure ki sab equipment check ho jaye")
        assertEquals("Kal morning ko OPD ka round hai please ensure ki sab equipment check ho jaye.", r.text)
        assertEquals(listOf("OPD"), r.fired)
    }

    @Test
    fun `spoken punctuation including new paragraph beats new line`() {
        val r = process(
            "dear sir comma please find the inspection report attached full stop new paragraph kindly approve at the earliest full stop"
        )
        assertEquals(
            "Dear sir, please find the inspection report attached.\n\nKindly approve at the earliest.",
            r.text,
        )
        assertEquals(emptyList<String>(), r.fired)
    }

    @Test
    fun `vocabulary fires in stored order not text-occurrence order`() {
        // Text occurrence order would be ICU then NQAS; stored order is NQAS (v3) then ICU (v8).
        val r = process("the i c u needs two more ventilators and the n quas audit is next week")
        assertEquals("The ICU needs two more ventilators and the NQAS audit is next week.", r.text)
        assertEquals(listOf("NQAS", "ICU"), r.fired)
    }

    @Test
    fun `question mark punctuation and vocabulary together`() {
        val r = process("lakshya certification review is due question mark")
        assertEquals("LaQshya certification review is due?", r.text)
        assertEquals(listOf("LaQshya"), r.fired)
    }

    @Test
    fun `semicolon spoken punctuation`() {
        val r = process("report is not ready yet semicolon please escalate")
        assertEquals("Report is not ready yet; please escalate.", r.text)
    }

    @Test
    fun `tidySpacing collapses runs of spaces and fixes punctuation spacing`() {
        val r = process("  multiple   spaces   and   trailing   punctuation ,  need   tidying   .  indeed")
        assertEquals("Multiple spaces and trailing punctuation, need tidying. Indeed.", r.text)
    }

    @Test
    fun `autoCaps capitalises after sentence punctuation and newline, not every word`() {
        val r = process("hello world. this should capitalise. and this too! does this? yes.\nnew sentence after newline")
        assertEquals(
            "Hello world. This should capitalise. And this too! Does this? Yes.\nNew sentence after newline.",
            r.text,
        )
    }

    @Test
    fun `autoPunct does not double a trailing period`() {
        val r = process("already ends with punctuation!")
        assertEquals("Already ends with punctuation!", r.text)
    }

    @Test
    fun `repeated punctuation phrases each count as a hit`() {
        val r = process("comma comma full stop")
        assertEquals(", , .", r.text)
    }

    @Test
    fun `spokenPunct disabled leaves phrases untouched`() {
        val r = postProcess("please add a comma here", PipelineSettings(spokenPunct = false), defaultVocab)
        assertEquals("Please add a comma here.", r.text)
    }

    @Test
    fun `autoCaps disabled leaves case untouched`() {
        val r = postProcess("lowercase stays lowercase.", PipelineSettings(autoCaps = false), emptyList())
        assertEquals("lowercase stays lowercase.", r.text)
    }

    @Test
    fun `snippet command matches case-insensitively and strips trailing punctuation`() {
        assertEquals(
            "Room 214, Administrative Block,\nDistrict Hospital, New Delhi — 110001",
            trySnippetCommand("insert address", defaultSnippets),
        )
        assertEquals(
            "Kind regards,\nR\nHospital Administration",
            trySnippetCommand("insert REGARDS.", defaultSnippets),
        )
    }

    @Test
    fun `snippet command returns null for unknown trigger or non-matching text`() {
        assertNull(trySnippetCommand("insert nonexistent snippet", defaultSnippets))
        assertNull(trySnippetCommand("not a command", defaultSnippets))
    }
}

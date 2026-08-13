package dev.devtoolbox.core

import dev.devtoolbox.core.tools.SubstringTool
import dev.devtoolbox.core.util.CodePoints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun slice(text: String, start: String, end: String): ToolBody.Substring {
    val out = SubstringTool.run(ToolInput.Slice(text, start, end))
    assertIs<ToolOutput.Success>(out)
    return out.body as ToolBody.Substring
}

class SubstringToolTest {

    @Test
    fun cutsTheRangeAskedFor() {
        val body = slice("DevToolbox", "3", "8")

        assertEquals("Toolb", body.result)
        assertEquals(3, body.appliedStart)
        assertEquals(8, body.appliedEnd)
    }

    @Test
    fun indicesBeyondTheEndAreClamped() {
        val body = slice("abc", "99", "150")

        assertEquals(3, body.appliedStart)
        assertEquals(3, body.appliedEnd)
        assertEquals("", body.result)
    }

    @Test
    fun negativeStartIsClampedToZero() {
        val body = slice("abc", "-5", "2")

        assertEquals(0, body.appliedStart)
        assertEquals(2, body.appliedEnd)
        assertEquals("ab", body.result)
    }

    @Test
    fun anEndBeforeTheStartIsAnEmptyRange() {
        val body = slice("DevToolbox", "6", "2")

        assertEquals(6, body.appliedStart)
        assertEquals(6, body.appliedEnd)
        assertEquals("", body.result)
    }

    @Test
    fun anEmptyEndMeansUntilTheEnd() {
        val body = slice("DevToolbox", "3", "")

        assertEquals(10, body.appliedEnd)
        assertEquals("Toolbox", body.result)
    }

    @Test
    fun aNonNumericValueCountsAsZero() {
        val body = slice("DevToolbox", "abc", "4")

        assertEquals(0, body.appliedStart)
        assertEquals(4, body.appliedEnd)
        assertEquals("DevT", body.result)
    }

    @Test
    fun whatWasTypedSurvivesTheSanitising() {
        val body = slice("abc", "-5", "99")

        assertEquals("-5", body.start, "o campo mantém o que foi digitado")
        assertEquals("99", body.end, "o campo mantém o que foi digitado")
        assertEquals(0, body.appliedStart)
        assertEquals(3, body.appliedEnd)
    }

    @Test
    fun accentedCharactersAreCutByCharacterNotByByte() {
        val body = slice("ação", "0", "3")

        assertEquals(4, body.length)
        assertEquals("açã", body.result)
    }

    @Test
    fun emojiCountAsOneCharacterEach() {
        val body = slice("a🙂b🚀c", "1", "3")

        assertEquals(5, body.length, "cada emoji conta como um caractere")
        assertEquals("🙂b", body.result)
    }

    @Test
    fun anEmojiIsNeverSplitInHalf() {
        val body = slice("🙂🚀", "0", "1")

        assertEquals(2, body.length)
        assertEquals("🙂", body.result)
    }

    @Test
    fun theResultLengthMatchesTheSanitisedRange() {
        val subjects = listOf("DevToolbox", "ação e emoji 🙂🚀 no meio")
        val ranges = listOf("0" to "4", "2" to "99", "-3" to "5", "7" to "2", "3" to "")

        for (subject in subjects) {
            for ((start, end) in ranges) {
                val body = slice(subject, start, end)
                val expected = body.appliedEnd - body.appliedStart
                assertEquals(
                    expected,
                    CodePoints.count(body.result),
                    "faixa [$start, $end) devia render $expected caracteres",
                )
            }
        }
    }

    @Test
    fun segmentsCoverTheWholeStringAndMarkOnlyTheSelection() {
        val body = slice("DevToolbox", "3", "8")

        assertEquals("DevToolbox", body.segments.joinToString("") { it.text })
        assertEquals("Toolb", body.segments.filter { it.matched }.joinToString("") { it.text })
    }

    @Test
    fun anEmptyStringIsHandled() {
        val body = slice("", "2", "5")

        assertEquals(0, body.length)
        assertEquals("", body.result)
        assertTrue(body.segments.isEmpty())
    }

    @Test
    fun theDefaultInputSelectsTheHost() {
        val out = SubstringTool.run(SubstringTool.defaultInput)

        assertIs<ToolOutput.Success>(out)
        assertEquals("devtoolbox.dev", (out.body as ToolBody.Substring).result)
    }

    @Test
    fun theWrongInputTypeFails() {
        assertIs<ToolOutput.Failure>(SubstringTool.run(ToolInput.Text("nope")))
    }
}

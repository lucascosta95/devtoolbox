package dev.devtoolbox.core

import dev.devtoolbox.core.tools.SubstringTool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun extract(
    text: String,
    start: String,
    length: String,
    multiLine: Boolean = false,
    reverse: Boolean = false,
): ToolBody.Substring {
    val out = SubstringTool.run(ToolInput.Slice(text, start, length, multiLine, reverse))
    assertIs<ToolOutput.Success>(out)
    return out.body as ToolBody.Substring
}

class SubstringToolTest {

    @Test
    fun positionsStartAtOne() {
        assertEquals("place like", extract("There's no place like home.", "12", "10").result)
        assertEquals("ui", extract("Penguin", "5", "2").result)
    }

    @Test
    fun aNegativeStartCountsFromTheEnd() {
        assertEquals("gu", extract("Penguin", "-4", "2").result)
        assertEquals("n", extract("Penguin", "-1", "1").result)
    }

    @Test
    fun aNegativeLengthCutsToTheLeftEndingAtTheStart() {
        assertEquals("en", extract("Penguin", "3", "-2").result)
        assertEquals("gui", extract("Penguin", "-2", "-3").result)
    }

    @Test
    fun anEmptyLengthGoesToTheEnd() {
        assertEquals("guin", extract("Penguin", "4", "").result)
        assertEquals("in", extract("Penguin", "-2", "").result)
    }

    @Test
    fun aBareMinusGoesToTheBeginning() {
        assertEquals("Peng", extract("Penguin", "4", "-").result)
        assertEquals("Pengui", extract("Penguin", "-2", "-").result)
    }

    @Test
    fun aPlusSignIsAccepted() {
        assertEquals("ui", extract("Penguin", "+5", "+2").result)
    }

    @Test
    fun aLengthPastTheEndKeepsWhatThereIsAndWarns() {
        val body = extract("Penguin", "6", "10")

        assertEquals("in", body.result)
        assertNotNull(body.warning)
    }

    @Test
    fun aLengthPastTheBeginningKeepsWhatThereIsAndWarns() {
        val body = extract("Penguin", "2", "-5")

        assertEquals("Pe", body.result)
        assertNotNull(body.warning)
    }

    @Test
    fun aCompleteCutHasNoWarning() {
        assertNull(extract("Penguin", "1", "7").warning)
    }

    @Test
    fun multiLineModeCutsEachLine() {
        val departures = """
            08:15  Rome      MK 1744  Boarding
            08:40  Oslo      SK 4711  Delayed
        """.trimIndent()

        val body = extract(departures, "24", "-7", multiLine = true)

        assertEquals("MK 1744\nSK 4711", body.result)
    }

    @Test
    fun multiLineModeNamesTheShortLines() {
        val body = extract("abcdef\nab\nabcdef", "2", "3", multiLine = true)

        assertEquals("bcd\nb\nbcd", body.result)
        assertEquals("Não havia caracteres suficientes na linha 2.", body.warning)
    }

    @Test
    fun withoutMultiLineTheTextIsOneString() {
        assertEquals("b\nc", extract("ab\ncd", "2", "3").result)
    }

    @Test
    fun reverseFlipsEachExtractedPiece() {
        val body = extract("a cat\na dog\na fox", "-1", "-3", multiLine = true, reverse = true)

        assertEquals("tac\ngod\nxof", body.result)
    }

    @Test
    fun segmentsCoverTheWholeTextAndMarkOnlyTheSelection() {
        val body = extract("abcdef\nabcdef", "2", "2", multiLine = true)

        assertEquals("abcdef\nabcdef", body.segments.joinToString("") { it.text })
        assertEquals("bcbc", body.segments.filter { it.matched }.joinToString("") { it.text })
    }

    @Test
    fun accentsAndEmojiCountAsOneCharacterEach() {
        assertEquals("çã", extract("ação", "2", "2").result)

        val body = extract("a🙂b🚀c", "2", "2")
        assertEquals(5, body.length)
        assertEquals("🙂b", body.result)
    }

    @Test
    fun anInvalidNumberFailsButKeepsTheText() {
        for ((start, length) in listOf("abc" to "2", "" to "2", "1" to "x", "1" to "2.5")) {
            val out = SubstringTool.run(ToolInput.Slice("Penguin", start, length))

            assertIs<ToolOutput.Failure>(out, "início '$start', comprimento '$length'")
            val body = assertIs<ToolBody.Substring>(out.body)
            assertEquals("Penguin", body.segments.joinToString("") { it.text })
            assertEquals("", body.result)
        }
    }

    @Test
    fun anEmptyStringIsHandled() {
        val body = extract("", "2", "5")

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

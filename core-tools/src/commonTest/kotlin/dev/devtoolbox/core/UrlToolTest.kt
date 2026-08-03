package dev.devtoolbox.core

import dev.devtoolbox.core.tools.UrlTool
import dev.devtoolbox.core.util.PercentEncoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UrlToolTest {

    @Test
    fun encodesThePrototypeExample() {
        val input = "https://devtoolbox.dev/search?q=kotlin multiplatform&lang=pt-BR"
        assertEquals(
            "https%3A%2F%2Fdevtoolbox.dev%2Fsearch%3Fq%3Dkotlin%20multiplatform%26lang%3Dpt-BR",
            PercentEncoding.encode(input),
        )
    }

    @Test
    fun keepsUnreservedCharacters() {
        assertEquals("abcXYZ019-._~", PercentEncoding.encode("abcXYZ019-._~"))
    }

    @Test
    fun roundTripsUtf8Multibyte() {
        val text = "ação & preço — 日本語"
        assertEquals(text, PercentEncoding.decode(PercentEncoding.encode(text)))
    }

    @Test
    fun decodesPlusAsSpace() {
        assertEquals("a b", PercentEncoding.decode("a+b"))
    }

    @Test
    fun rejectsTruncatedEscape() {
        assertFailsWith<PercentEncoding.DecodeException> { PercentEncoding.decode("abc%4") }
    }

    @Test
    fun rejectsNonHexEscape() {
        assertFailsWith<PercentEncoding.DecodeException> { PercentEncoding.decode("abc%ZZdef") }
    }

    @Test
    fun autoModeDecodesWhenItSeesPercentSequences() {
        val out = UrlTool.run(ToolInput.Text("a%20b"))
        assertIs<ToolOutput.Success>(out)
        assertEquals("a b", (out.body as ToolBody.Io).output)
    }

    @Test
    fun autoModeEncodesPlainText() {
        val out = UrlTool.run(ToolInput.Text("a b"))
        assertIs<ToolOutput.Success>(out)
        assertEquals("a%20b", (out.body as ToolBody.Io).output)
    }

    @Test
    fun reportsFailureOnBrokenEscape() {
        val out = UrlTool.run(ToolInput.Text("%ZZ", Direction.Decode))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("URL inválida"))
    }
}

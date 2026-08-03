package dev.devtoolbox.core

import dev.devtoolbox.core.tools.Base64Tool
import dev.devtoolbox.core.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Base64ToolTest {

    @Test
    fun encodesTheProtoypeExample() {
        assertEquals("SGVsbG8sIFdvcmxkIQ==", Base64.encode("Hello, World!"))
    }

    @Test
    fun matchesRfc4648TestVectors() {
        assertEquals("", Base64.encode(""))
        assertEquals("Zg==", Base64.encode("f"))
        assertEquals("Zm8=", Base64.encode("fo"))
        assertEquals("Zm9v", Base64.encode("foo"))
        assertEquals("Zm9vYg==", Base64.encode("foob"))
        assertEquals("Zm9vYmE=", Base64.encode("fooba"))
        assertEquals("Zm9vYmFy", Base64.encode("foobar"))
    }

    @Test
    fun roundTripsUtf8Multibyte() {
        val text = "Ação — çãé 日本語 🎉"
        assertEquals(text, Base64.decode(Base64.encode(text)))
    }

    @Test
    fun decodesWithoutPadding() {
        assertEquals("foobar", Base64.decode("Zm9vYmFy"))
        assertEquals("fooba", Base64.decode("Zm9vYmE"))
        assertEquals("foob", Base64.decode("Zm9vYg"))
    }

    @Test
    fun decodesUrlSafeAlphabet() {
        val bytes = byteArrayOf(0xFB.toByte(), 0xFF.toByte(), 0xBE.toByte())
        val urlSafe = Base64.encodeBytes(bytes, urlSafe = true)
        assertTrue(urlSafe.none { it == '+' || it == '/' })
        assertEquals(bytes.toList(), Base64.decodeToBytes(urlSafe).toList())
    }

    @Test
    fun rejectsInvalidCharacters() {
        assertFailsWith<Base64.DecodeException> { Base64.decodeToBytes("abc!def") }
    }

    @Test
    fun rejectsInvalidLength() {
        assertFailsWith<Base64.DecodeException> { Base64.decodeToBytes("Zm9vYmFyZ") }
    }

    @Test
    fun autoModeEncodesPlainTextAndDecodesBase64() {
        val encoded = Base64Tool.run(ToolInput.Text("Hello, World!"))
        assertIs<ToolOutput.Success>(encoded)
        assertEquals("SGVsbG8sIFdvcmxkIQ==", (encoded.body as ToolBody.Io).output)

        val decoded = Base64Tool.run(ToolInput.Text("SGVsbG8sIFdvcmxkIQ=="))
        assertIs<ToolOutput.Success>(decoded)
        assertEquals("Hello, World!", (decoded.body as ToolBody.Io).output)
    }

    @Test
    fun explicitDirectionOverridesTheHeuristic() {
        // "Zm9v" é Base64 válido, mas no modo Encode deve ser codificado como texto.
        val out = Base64Tool.run(ToolInput.Text("Zm9v", Direction.Encode))
        assertIs<ToolOutput.Success>(out)
        assertEquals("Wm05dg==", (out.body as ToolBody.Io).output)
    }

    @Test
    fun reportsFailureWhenDecodingGarbage() {
        val out = Base64Tool.run(ToolInput.Text("não é base64!!", Direction.Decode))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("Base64 inválido"))
    }

    @Test
    fun emptyInputProducesEmptyOutput() {
        val out = Base64Tool.run(ToolInput.Text(""))
        assertIs<ToolOutput.Success>(out)
        assertEquals("", (out.body as ToolBody.Io).output)
    }
}

package dev.devtoolbox.core

import dev.devtoolbox.core.tools.JsonStringParserTool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val EXPECTED = """{
  "orderId": "A-8842",
  "total": 149.9
}"""

private fun parse(text: String): ToolOutput = JsonStringParserTool.run(ToolInput.Text(text))

private fun output(text: String): String {
    val out = parse(text)
    assertIs<ToolOutput.Success>(out, "esperava sucesso para: $text")
    return (out.body as ToolBody.Io).output
}

class JsonStringParserToolTest {

    @Test
    fun theThreeShapesOfTheSameContentProduceTheSameOutput() {
        val quotedAndEscaped = "\"" + """{\"orderId\":\"A-8842\",\"total\":149.9}""" + "\""
        val escapedOnly = """{\"orderId\":\"A-8842\",\"total\":149.9}"""
        val plainJson = """{"orderId":"A-8842","total":149.9}"""

        assertEquals(EXPECTED, output(quotedAndEscaped))
        assertEquals(EXPECTED, output(escapedOnly))
        assertEquals(EXPECTED, output(plainJson))
    }

    @Test
    fun surroundingWhitespaceIsIgnored() {
        val padded = "   " + "\"" + """{\"orderId\":\"A-8842\",\"total\":149.9}""" + "\"" + "  \n"
        assertEquals(EXPECTED, output(padded))
    }

    @Test
    fun alreadyFormattedJsonPassesThrough() {
        assertEquals(EXPECTED, output(EXPECTED))
    }

    @Test
    fun escapedNewlineTabAndUnicodeSurviveInsideValues() {
        val input = "\"" + """{\"note\":\"linha1\\nlinha2\\tfim\",\"who\":\"Ana\\u00e7\"}""" + "\""

        val out = output(input)

        assertTrue(out.contains("""\n"""), "o \\n precisa continuar escapado na saída: $out")
        assertTrue(out.contains("""\t"""), "o \\t precisa continuar escapado na saída: $out")
        assertTrue(out.contains("Anaç"), "o \\u00e7 precisa virar ç: $out")
    }

    @Test
    fun aBadlyEscapedStringFails() {
        val out = parse("\"" + """{\"a\": \q}""" + "\"")

        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.isNotBlank())
    }

    @Test
    fun invalidJsonInsideTheStringFails() {
        val out = parse("\"" + """{\"a\" 1}""" + "\"")

        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.startsWith("JSON inválido"), out.message)
    }

    @Test
    fun aFailureKeepsTheTypedInputAndShowsTheMessageInTheOutputPanel() {
        val typed = "\"" + """{\"a\" 1}""" + "\""

        val out = parse(typed)

        assertIs<ToolOutput.Failure>(out)
        val body = out.body as ToolBody.Io
        assertEquals(typed, body.input, "a entrada digitada não pode ser limpa")
        assertTrue(body.output.startsWith("Entrada inválida: "), body.output)
    }

    @Test
    fun blankInputProducesAnEmptyOutputWithoutFailing() {
        val out = parse("   ")

        assertIs<ToolOutput.Success>(out)
        assertEquals("", (out.body as ToolBody.Io).output)
    }

    @Test
    fun theDefaultInputParses() {
        val out = JsonStringParserTool.run(JsonStringParserTool.defaultInput)

        assertIs<ToolOutput.Success>(out)
        val body = out.body as ToolBody.Io
        assertTrue(body.output.contains("\"orderId\": \"A-8842\""), body.output)
        assertTrue(body.output.contains("\"sku\": \"KT-01\""), body.output)
    }
}

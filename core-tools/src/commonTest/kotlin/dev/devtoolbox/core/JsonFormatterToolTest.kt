package dev.devtoolbox.core

import dev.devtoolbox.core.tools.JsonFormatterTool
import dev.devtoolbox.core.util.Json
import dev.devtoolbox.core.util.JsonParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsonFormatterToolTest {

    @Test
    fun formatsThePrototypeExample() {
        val input = """{"name":"Ana Souza","role":"Backend Developer","active":true,""" +
            """"skills":["Kotlin","Ktor","Postgres"],"team":{"name":"Platform","size":6}}"""
        val expected = """
            {
              "name": "Ana Souza",
              "role": "Backend Developer",
              "active": true,
              "skills": [
                "Kotlin",
                "Ktor",
                "Postgres"
              ],
              "team": {
                "name": "Platform",
                "size": 6
              }
            }
        """.trimIndent()
        assertEquals(expected, Json.format(input))
    }

    @Test
    fun preservesKeyOrder() {
        assertEquals("""{"z":1,"a":2,"m":3}""", Json.compact(Json.parse("""{"z":1,"a":2,"m":3}""")))
    }

    @Test
    fun preservesNumberLiterals() {
        assertEquals("""[1.0,1e5,-0.5,1234567890123456789]""",
            Json.compact(Json.parse("""[1.0, 1e5, -0.5, 1234567890123456789]""")))
    }

    @Test
    fun formattingIsIdempotent() {
        val once = Json.format("""{"a":[1,{"b":null}],"c":false}""")
        assertEquals(once, Json.format(once))
    }

    @Test
    fun handlesEmptyContainers() {
        assertEquals("""{"a":[],"b":{}}""", Json.compact(Json.parse("""{"a": [], "b": {}}""")))
    }

    @Test
    fun roundTripsEscapesAndUnicode() {
        val text = """{"s":"linha\nquebra \"aspas\" \\ barra \t tab çãé"}"""
        assertEquals(text, Json.compact(Json.parse(text)))
    }

    @Test
    fun reportsPositionOnMalformedJson() {
        val e = assertFailsWith<JsonParseException> { Json.parse("{\n  \"a\": 1,\n  \"b\" 2\n}") }
        assertTrue(e.message!!.contains("linha 3"), "mensagem sem a linha: ${e.message}")
    }

    @Test
    fun rejectsTrailingContent() {
        assertFailsWith<JsonParseException> { Json.parse("""{"a":1} lixo""") }
    }

    @Test
    fun rejectsUnterminatedString() {
        assertFailsWith<JsonParseException> { Json.parse("""{"a":"abc}""") }
    }

    @Test
    fun toolReturnsFailureWithMessage() {
        val out = JsonFormatterTool.run(ToolInput.Text("{"))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("JSON inválido"))
    }

    @Test
    fun blankInputProducesEmptyOutput() {
        val out = JsonFormatterTool.run(ToolInput.Text("   "))
        assertIs<ToolOutput.Success>(out)
        assertEquals("", (out.body as ToolBody.Io).output)
    }
}

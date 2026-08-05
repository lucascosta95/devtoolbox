package dev.devtoolbox.core

import dev.devtoolbox.core.tools.CurlFormatterTool
import dev.devtoolbox.core.tools.DiffTool
import dev.devtoolbox.core.tools.JwtTool
import dev.devtoolbox.core.tools.YamlFormatterTool
import dev.devtoolbox.core.util.Curl
import dev.devtoolbox.core.util.Diff
import dev.devtoolbox.core.util.Yaml
import dev.devtoolbox.core.util.YamlParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class YamlFormatterTest {

    @Test
    fun convertsFlowToBlock() {
        val input = "name: Ana Souza\n" +
            "role: Backend Developer\n" +
            "skills: [Kotlin, Ktor, Postgres]\n" +
            "team: {name: Platform, size: 6}"
        val expected = """
            name: Ana Souza
            role: Backend Developer
            skills:
              - Kotlin
              - Ktor
              - Postgres
            team:
              name: Platform
              size: 6
        """.trimIndent()
        assertEquals(expected, Yaml.format(input))
    }

    @Test
    fun formattingIsIdempotent() {
        val once = Yaml.format("a: {b: 1, c: [2, 3]}")
        assertEquals(once, Yaml.format(once))
    }

    @Test
    fun keepsSequencesOfMaps() {
        val input = "servers:\n  - name: web\n    port: 80\n  - name: db\n    port: 5432"
        val output = Yaml.format(input)
        assertTrue(output.contains("- \n") || output.contains("-\n"), "saída:\n$output")
        assertTrue(output.contains("name: web"))
        assertTrue(output.contains("port: 5432"))
    }

    @Test
    fun ignoresComments() {
        assertEquals("a: 1", Yaml.format("# comentário\na: 1 # fim"))
    }

    @Test
    fun rejectsAnchors() {
        val e = assertFailsWith<YamlParseException> { Yaml.parse("base: &ref\n  a: 1") }
        assertTrue(e.message!!.contains("âncoras"))
    }

    @Test
    fun rejectsMultipleDocuments() {
        val e = assertFailsWith<YamlParseException> { Yaml.parse("---\na: 1\n---\nb: 2") }
        assertTrue(e.message!!.contains("documentos"))
    }

    @Test
    fun rejectsBlockScalars() {
        val e = assertFailsWith<YamlParseException> { Yaml.parse("script: |\n  echo oi") }
        assertTrue(e.message!!.contains("bloco"))
    }

    @Test
    fun toolReportsFailure() {
        val out = YamlFormatterTool.run(ToolInput.Text("a: &x 1"))
        assertIs<ToolOutput.Failure>(out)
    }
}

class CurlFormatterTest {

    @Test
    fun breaksThePrototypeExample() {
        val input = "curl -X POST https://api.devtoolbox.dev/v1/tokens " +
            "-H \"Content-Type: application/json\" " +
            "-H \"Authorization: Bearer abc123\" " +
            "-d '{\"grant_type\":\"client_credentials\"}'"
        val output = Curl.format(input)
        val lines = output.lines()
        assertEquals("curl -X POST https://api.devtoolbox.dev/v1/tokens \\", lines[0])
        assertTrue(lines[1].startsWith("  -H "))
        assertTrue(lines.last().contains("grant_type"))
        assertEquals(4, lines.size)
    }

    @Test
    fun tokenizesNestedQuotes() {
        val tokens = Curl.tokenize("""curl -d '{"a":"b c"}' https://x.dev""")
        assertEquals(listOf("curl", "-d", """{"a":"b c"}""", "https://x.dev"), tokens)
    }

    @Test
    fun handlesLineContinuations() {
        val tokens = Curl.tokenize("curl \\\n  -X GET \\\n  https://x.dev")
        assertEquals(listOf("curl", "-X", "GET", "https://x.dev"), tokens)
    }

    @Test
    fun rejectsUnclosedQuotes() {
        assertFailsWith<Curl.ParseException> { Curl.tokenize("""curl -d "abc""") }
    }

    @Test
    fun rejectsCommandsThatDoNotStartWithCurl() {
        val out = CurlFormatterTool.run(ToolInput.Text("wget https://x.dev"))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("curl"))
    }
}

class DiffTest {

    @Test
    fun marksAdditionsAndDeletions() {
        val lines = Diff.lines("a\nb\nc", "a\nB\nc")
        assertEquals(
            listOf(DiffKind.Same, DiffKind.Del, DiffKind.Add, DiffKind.Same),
            lines.map { it.kind },
        )
        assertEquals("b", lines[1].text)
        assertEquals("B", lines[2].text)
    }

    @Test
    fun identicalInputsHaveNoChanges() {
        val lines = Diff.lines("a\nb", "a\nb")
        assertTrue(lines.all { it.kind == DiffKind.Same })
    }

    @Test
    fun handlesPureInsertion() {
        val lines = Diff.lines("a\nc", "a\nb\nc")
        assertEquals(1, lines.count { it.kind == DiffKind.Add })
        assertEquals(0, lines.count { it.kind == DiffKind.Del })
    }

    @Test
    fun handlesEmptySide() {
        val lines = Diff.lines("", "a\nb")
        assertTrue(lines.any { it.kind == DiffKind.Add })
    }

    @Test
    fun normalizesJsonBeforeComparing() {
        val out = DiffTool.run(ToolInput.Pair("""{"a":1,"b":2}""", "{\n  \"a\": 1,\n  \"b\": 2\n}"))
        assertIs<ToolOutput.Success>(out)
        val body = out.body as ToolBody.Diff
        assertTrue(body.lines.all { it.kind == DiffKind.Same }, "linhas: ${body.lines}")
    }

    @Test
    fun deletionsComeBeforeAdditionsInABlock() {
        val out = DiffTool.run(DiffTool.defaultInput)
        assertIs<ToolOutput.Success>(out)
        val kinds = (out.body as ToolBody.Diff).lines.map { it.kind }
        for (i in 0 until kinds.size - 1) {
            if (kinds[i] == DiffKind.Add) {
                assertTrue(kinds[i + 1] != DiffKind.Del, "remoção depois de adição no mesmo bloco")
            }
        }
    }
}

class JwtToolTest {

    @Test
    fun decodesHeaderAndPayload() {
        val out = JwtTool.run(JwtTool.defaultInput)
        assertIs<ToolOutput.Success>(out)
        val body = out.body as ToolBody.Jwt
        assertTrue(body.headerJson.contains("\"alg\": \"HS256\""))
        assertTrue(body.payloadJson.contains("\"name\": \"John Doe\""))
        assertTrue(body.signaturePart.isNotEmpty())
    }

    @Test
    fun rejectsWrongNumberOfParts() {
        for (token in listOf("a.b", "a.b.c.d")) {
            val out = JwtTool.run(ToolInput.Text(token))
            assertIs<ToolOutput.Failure>(out)
            assertTrue(out.message.contains("3 partes"))
        }
    }

    @Test
    fun rejectsCorruptedBase64() {
        val out = JwtTool.run(ToolInput.Text("!!!.eyJhIjoxfQ.sig"))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("Base64URL"))
    }

    @Test
    fun rejectsPayloadThatIsNotJson() {
        val out = JwtTool.run(ToolInput.Text("eyJhbGciOiJIUzI1NiJ9.bm90IGpzb24.sig"))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("JSON"))
    }

    @Test
    fun acceptsTokensWithoutPadding() {
        val out = JwtTool.run(ToolInput.Text("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc"))
        assertIs<ToolOutput.Success>(out)
    }
}

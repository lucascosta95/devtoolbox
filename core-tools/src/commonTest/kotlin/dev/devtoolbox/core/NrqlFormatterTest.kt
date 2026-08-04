package dev.devtoolbox.core

import dev.devtoolbox.core.tools.NrqlFormatterTool
import dev.devtoolbox.core.util.NrqlFormatResult
import dev.devtoolbox.core.util.formatNrql
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/** Consulta do protótipo, também usada como entrada padrão da ferramenta. */
private const val REFERENCE_INPUT =
    "select count(*), average(duration) from Transaction " +
        "where appName = 'checkout-api' and httpResponseCode != '200' " +
        "facet name, host since 3 hours ago until 30 minutes ago " +
        "timeseries 5 minutes limit 50"

private val REFERENCE_OUTPUT = """
    SELECT
      count(*),
      average(duration)
    FROM Transaction
    WHERE appName = 'checkout-api'
      AND httpResponseCode != '200'
    FACET
      name,
      host
    SINCE 3 hours ago
    UNTIL 30 minutes ago
    TIMESERIES 5 minutes
    LIMIT 50
""".trimIndent()

private fun nrql(input: String): String = when (val result = formatNrql(input)) {
    is NrqlFormatResult.Success -> result.nrql
    is NrqlFormatResult.Failure -> fail("formatNrql falhou: ${result.message}")
}

class NrqlFormatterTest {

    @Test
    fun formatsThePrototypeExample() {
        assertEquals(REFERENCE_OUTPUT, nrql(REFERENCE_INPUT))
    }

    @Test
    fun keepsASingleProjectionInline() {
        val expected = """
            SELECT uniqueCount(userId)
            FROM Transaction
            SINCE 1 day ago
        """.trimIndent()
        assertEquals(expected, nrql("select uniqueCount(userId) from Transaction since 1 day ago"))
    }

    @Test
    fun preservesFunctionCamelCase() {
        val output = nrql("select uniqueCount(userId), getField(payload, id) from Log")
        assertTrue(output.contains("uniqueCount(userId)"), output)
        assertTrue(output.contains("getField(payload, id)"), output)
    }

    @Test
    fun indentsCasesInsideFacet() {
        val expected = """
            SELECT count(*)
            FROM Transaction
            FACET cases(
              WHERE duration < 1 AS rapido,
              WHERE duration >= 1 AS lento
            )
            SINCE 1 hour ago
        """.trimIndent()
        val input = "select count(*) from Transaction " +
            "facet cases(where duration < 1 as rapido, where duration >= 1 as lento) since 1 hour ago"
        assertEquals(expected, nrql(input))
    }

    @Test
    fun keepsQuotedTimestamps() {
        val expected = """
            SELECT count(*)
            FROM Log
            SINCE '2026-01-01 00:00:00'
            UNTIL '2026-01-02 00:00:00'
        """.trimIndent()
        val input = "select count(*) from Log since '2026-01-01 00:00:00' until '2026-01-02 00:00:00'"
        assertEquals(expected, nrql(input))
    }

    @Test
    fun writesTimeWindowsInTheirNaturalForm() {
        val expected = """
            SELECT average(duration)
            FROM Transaction
            SINCE 1 week ago
            COMPARE WITH 1 week ago
            TIMESERIES AUTO
        """.trimIndent()
        val input = "select average(duration) from Transaction " +
            "since 1 WEEK AGO compare with 1 week ago timeseries auto"
        assertEquals(expected, nrql(input))
    }

    @Test
    fun indentsNestedFilter() {
        val expected = """
            SELECT filter(
              count(*),
              WHERE httpResponseCode = '500'
            ) AS erros
            FROM Transaction
            FACET appName
        """.trimIndent()
        val input = "select filter(count(*), where httpResponseCode = '500') as erros " +
            "from Transaction facet appName"
        assertEquals(expected, nrql(input))
    }

    @Test
    fun indentsSubqueryInFrom() {
        val expected = """
            SELECT max(total)
            FROM (
              SELECT count(*) AS total
              FROM Transaction
              FACET appName
            )
            SINCE 1 day ago
        """.trimIndent()
        val input = "select max(total) from (select count(*) as total from Transaction " +
            "facet appName) since 1 day ago"
        assertEquals(expected, nrql(input))
    }

    @Test
    fun leavesStringLiteralsAlone() {
        val expected = """
            SELECT count(*)
            FROM Log
            WHERE message LIKE '%where clause%'
            SINCE 30 minutes ago
        """.trimIndent()
        val input = "select count(*) from Log where message like '%where clause%' since 30 minutes ago"
        assertEquals(expected, nrql(input))
    }

    @Test
    fun keepsAttributeAndEventCapitalization() {
        val output = nrql("select count(*) from Transaction where appName = 'x' and httpResponseCode = '200'")
        assertTrue(output.contains("FROM Transaction"), output)
        assertTrue(output.contains("WHERE appName = 'x'"), output)
        assertTrue(output.contains("AND httpResponseCode = '200'"), output)
    }

    @Test
    fun uppercasesTheLimitMaxLiteralButNotTheMaxFunction() {
        assertTrue(nrql("select count(*) from Log limit max").endsWith("LIMIT MAX"))
        assertTrue(nrql("select max(duration) from Log").contains("max(duration)"))
    }

    @Test
    fun normalizesInputItCannotRecognize() {
        assertEquals("isto nao e nrql", nrql("isto   nao   e    nrql"))
    }

    @Test
    fun formattingIsIdempotent() {
        val cases = listOf(
            REFERENCE_INPUT,
            "select filter(count(*), where x = 1) from Transaction facet a, b since 1 hour ago",
            "select max(total) from (select count(*) as total from Log facet host) timeseries auto",
            "select count(*) from Log where a = 1 or b = 2 compare with 1 week ago",
        )
        for (input in cases) {
            val once = nrql(input)
            assertEquals(once, nrql(once), "não idempotente para: $input")
        }
    }

    @Test
    fun rejectsEmptyInput() {
        for (input in listOf("", "   ", "\n\t")) {
            val result = formatNrql(input)
            assertIs<NrqlFormatResult.Failure>(result, "esperava falha para «$input»")
            assertTrue(result.message.isNotBlank())
        }
    }

    @Test
    fun toolExposesTheIoBody() {
        val output = NrqlFormatterTool.run(NrqlFormatterTool.defaultInput)
        assertIs<ToolOutput.Success>(output)
        val body = output.body as ToolBody.Io
        assertEquals(REFERENCE_OUTPUT, body.output)
        assertEquals("NRQL", body.inputLabel)
    }

    @Test
    fun toolKeepsBlankInputEmptyLikeTheOtherFormatters() {
        val output = NrqlFormatterTool.run(ToolInput.Text(""))
        assertIs<ToolOutput.Success>(output)
        assertEquals("", (output.body as ToolBody.Io).output)
    }
}

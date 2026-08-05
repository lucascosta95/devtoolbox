package dev.devtoolbox.core

import dev.devtoolbox.core.tools.SqlFormatterTool
import dev.devtoolbox.core.util.SqlFormatResult
import dev.devtoolbox.core.util.formatSql
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

private const val REFERENCE_INPUT =
    "select u.id, u.name, count(o.id) as total from users u " +
        "inner join orders o on o.user_id = u.id " +
        "where u.active = true and o.created_at >= '2026-01-01' " +
        "group by u.id, u.name having count(o.id) > 3 order by total desc limit 20;"

private val REFERENCE_OUTPUT = """
    SELECT
      u.id,
      u.name,
      COUNT(o.id) AS total
    FROM users u
      INNER JOIN orders o
        ON o.user_id = u.id
    WHERE u.active = TRUE
      AND o.created_at >= '2026-01-01'
    GROUP BY
      u.id,
      u.name
    HAVING COUNT(o.id) > 3
    ORDER BY total DESC
    LIMIT 20;
""".trimIndent()

private fun sql(input: String): String = when (val result = formatSql(input)) {
    is SqlFormatResult.Success -> result.sql
    is SqlFormatResult.Failure -> fail("formatSql falhou: ${result.message}")
}

class SqlFormatterTest {

    @Test
    fun formatsThePrototypeExample() {
        assertEquals(REFERENCE_OUTPUT, sql(REFERENCE_INPUT))
    }

    @Test
    fun indentsJoinAndItsUsing() {
        val expected = """
            SELECT a.id
            FROM accounts a
              LEFT JOIN owners o
                USING (account_id)
            WHERE a.status = 'open'
        """.trimIndent()
        val input = "select a.id from accounts a left join owners o using (account_id) " +
            "where a.status = 'open'"
        assertEquals(expected, sql(input))
    }

    @Test
    fun opensALevelForASubquery() {
        val expected = """
            SELECT id
            FROM users
            WHERE id IN (
              SELECT user_id
              FROM orders
              WHERE total > 100
            )
              AND active = TRUE
        """.trimIndent()
        val input = "select id from users where id in " +
            "(select user_id from orders where total > 100) and active = true"
        assertEquals(expected, sql(input))
    }

    @Test
    fun separatesCtesWithABlankLine() {
        val expected = """
            WITH ativos AS (
              SELECT id
              FROM users
              WHERE active = TRUE
            ),

            recentes AS (
              SELECT user_id
              FROM orders
              WHERE created_at >= '2026-01-01'
            )
            SELECT a.id
            FROM ativos a
              JOIN recentes r
                ON r.user_id = a.id
        """.trimIndent()
        val input = "with ativos as (select id from users where active = true), " +
            "recentes as (select user_id from orders where created_at >= '2026-01-01') " +
            "select a.id from ativos a join recentes r on r.user_id = a.id"
        assertEquals(expected, sql(input))
    }

    @Test
    fun breaksCaseIntoOneLinePerBranch() {
        val expected = """
            SELECT
              id,
              CASE
                WHEN total > 100
                THEN 'alto'
                WHEN total > 50
                THEN 'medio'
                ELSE 'baixo'
              END AS faixa
            FROM orders
        """.trimIndent()
        val input = "select id, case when total > 100 then 'alto' " +
            "when total > 50 then 'medio' else 'baixo' end as faixa from orders"
        assertEquals(expected, sql(input))
    }

    @Test
    fun leavesStringLiteralsAlone() {
        val expected = """
            SELECT
              'select * from users' AS literal,
              name
            FROM t
        """.trimIndent()
        assertEquals(expected, sql("select 'select * from users' as literal, name from t"))
    }

    @Test
    fun keepsLineCommentsAtTheEndOfTheirLine() {
        val expected = """
            SELECT id
            FROM users
            WHERE active = TRUE -- somente ativos
        """.trimIndent()
        assertEquals(expected, sql("select id from users where active = true -- somente ativos"))
    }

    @Test
    fun formatsInsertWithValues() {
        val expected = """
            INSERT INTO users (id, name)
            VALUES (1, 'Ana'), (2, 'Bruno');
        """.trimIndent()
        assertEquals(expected, sql("insert into users (id, name) values (1, 'Ana'), (2, 'Bruno');"))
    }

    @Test
    fun formatsUpdateWithSet() {
        val expected = """
            UPDATE users
            SET active = FALSE, updated_at = '2026-01-01'
            WHERE id = 7
        """.trimIndent()
        val input = "update users set active = false, updated_at = '2026-01-01' where id = 7"
        assertEquals(expected, sql(input))
    }

    @Test
    fun separatesStatementsWithABlankLine() {
        assertEquals("SELECT 1;\n\nSELECT 2;", sql("select 1; select 2;"))
    }

    @Test
    fun keepsBetweenOnASingleLine() {
        val expected = """
            SELECT id
            FROM t
            WHERE d BETWEEN '2026-01-01' AND '2026-02-01'
              AND x = 1
        """.trimIndent()
        assertEquals(expected, sql("select id from t where d between '2026-01-01' and '2026-02-01' and x = 1"))
    }

    @Test
    fun normalizesInputItCannotRecognize() {
        assertEquals("isto nao e sql", sql("isto   nao   e    sql"))
    }

    @Test
    fun formattingIsIdempotent() {
        val cases = listOf(
            REFERENCE_INPUT,
            "with a as (select 1), b as (select 2) select * from a",
            "select case when x then y else z end as c, d from t where a = 1 and b = 2",
            "select id from users where id in (select user_id from orders) -- filtro",
        )
        for (input in cases) {
            val once = sql(input)
            assertEquals(once, sql(once), "não idempotente para: $input")
        }
    }

    @Test
    fun rejectsEmptyInput() {
        for (input in listOf("", "   ", "\n\t")) {
            val result = formatSql(input)
            assertIs<SqlFormatResult.Failure>(result, "esperava falha para «$input»")
            assertTrue(result.message.isNotBlank())
        }
    }

    @Test
    fun toolExposesTheIoBody() {
        val output = SqlFormatterTool.run(SqlFormatterTool.defaultInput)
        assertIs<ToolOutput.Success>(output)
        val body = output.body as ToolBody.Io
        assertEquals(REFERENCE_OUTPUT, body.output)
        assertEquals("SQL", body.inputLabel)
    }

    @Test
    fun toolKeepsBlankInputEmptyLikeTheOtherFormatters() {
        val output = SqlFormatterTool.run(ToolInput.Text(""))
        assertIs<ToolOutput.Success>(output)
        assertEquals("", (output.body as ToolBody.Io).output)
    }
}

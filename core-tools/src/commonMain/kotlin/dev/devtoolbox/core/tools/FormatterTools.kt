package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.Curl
import dev.devtoolbox.core.util.Diff
import dev.devtoolbox.core.util.NrqlFormatResult
import dev.devtoolbox.core.util.SqlFormatResult
import dev.devtoolbox.core.util.Yaml
import dev.devtoolbox.core.util.YamlParseException
import dev.devtoolbox.core.util.formatNrql
import dev.devtoolbox.core.util.formatSql

object YamlFormatterTool : Tool {
    override val id = "yaml"
    override val name = "YAML Formatter"
    override val category = Category.Formatters
    override val icon = "file-text"
    override val description = "Reorganize YAML em estilo de bloco, limpo e consistente."
    override val defaultInput = ToolInput.Text(
        "name: Ana Souza\n" +
            "role: Backend Developer\n" +
            "skills: [Kotlin, Ktor, Postgres]\n" +
            "team: {name: Platform, size: 6}",
    )

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isBlank()) return ToolOutput.Success(ToolBody.Io(text, "", "YAML", "YAML formatado"))
        return try {
            ToolOutput.Success(ToolBody.Io(text, Yaml.format(text), "YAML", "YAML formatado"))
        } catch (e: YamlParseException) {
            ToolOutput.Failure(e.message ?: "YAML inválido.")
        }
    }
}

object CurlFormatterTool : Tool {
    override val id = "curl"
    override val name = "cURL Formatter"
    override val category = Category.Formatters
    override val icon = "terminal-window"
    override val description = "Quebre um comando cURL em linhas legíveis."
    override val defaultInput = ToolInput.Text(
        "curl -X POST https://api.devtoolbox.dev/v1/tokens " +
            "-H \"Content-Type: application/json\" " +
            "-H \"Authorization: Bearer abc123\" " +
            "-d '{\"grant_type\":\"client_credentials\"}'",
    )

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isBlank()) return ToolOutput.Success(ToolBody.Io(text, "", "Comando", "Formatado"))
        return try {
            ToolOutput.Success(ToolBody.Io(text, Curl.format(text), "Comando", "Formatado"))
        } catch (e: Curl.ParseException) {
            ToolOutput.Failure("cURL inválido: ${e.message}")
        }
    }
}

object SqlFormatterTool : Tool {
    override val id = "sql"
    override val name = "SQL Formatter"
    override val category = Category.Formatters
    override val icon = "database"
    override val description = "Indente consultas SQL com palavras-chave em maiúsculas."
    override val defaultInput = ToolInput.Text(
        "select u.id, u.name, count(o.id) as total from users u " +
            "inner join orders o on o.user_id = u.id " +
            "where u.active = true and o.created_at >= '2026-01-01' " +
            "group by u.id, u.name having count(o.id) > 3 order by total desc limit 20;",
    )

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isBlank()) return ToolOutput.Success(ToolBody.Io(text, "", "SQL", "SQL formatado"))
        return when (val result = formatSql(text)) {
            is SqlFormatResult.Success ->
                ToolOutput.Success(ToolBody.Io(text, result.sql, "SQL", "SQL formatado"))
            is SqlFormatResult.Failure -> ToolOutput.Failure(result.message)
        }
    }
}

object NrqlFormatterTool : Tool {
    override val id = "nrql"
    override val name = "NRQL Formatter"
    override val category = Category.Formatters
    override val icon = "chart-line"
    override val description = "Indente consultas NRQL (New Relic) e similares de observabilidade."
    override val defaultInput = ToolInput.Text(
        "select count(*), average(duration) from Transaction " +
            "where appName = 'checkout-api' and httpResponseCode != '200' " +
            "facet name, host since 3 hours ago until 30 minutes ago " +
            "timeseries 5 minutes limit 50",
    )

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isBlank()) return ToolOutput.Success(ToolBody.Io(text, "", "NRQL", "NRQL formatado"))
        return when (val result = formatNrql(text)) {
            is NrqlFormatResult.Success ->
                ToolOutput.Success(ToolBody.Io(text, result.nrql, "NRQL", "NRQL formatado"))
            is NrqlFormatResult.Failure -> ToolOutput.Failure(result.message)
        }
    }
}

object DiffTool : Tool {
    override val id = "diff"
    override val name = "Text/JSON Diff"
    override val category = Category.Formatters
    override val icon = "arrows-split"
    override val description = "Compare dois blocos de texto ou JSON lado a lado."
    override val defaultInput = ToolInput.Pair(
        left = "{\n  \"env\": \"production\",\n  \"debug\": true,\n  \"timeout\": 30,\n  \"retries\": 2\n}",
        right = "{\n  \"env\": \"production\",\n  \"debug\": false,\n  \"timeout\": 30,\n  \"retries\": 5\n}",
    )

    override fun run(input: ToolInput): ToolOutput {
        val pair = input as? ToolInput.Pair ?: return ToolOutput.Failure("Entrada inválida.")
        // Quando os dois lados são JSON, compara a estrutura formatada — não a formatação.
        val (left, right) = Diff.normalizeJsonIfPossible(pair.left, pair.right)
        return ToolOutput.Success(ToolBody.Diff(Diff.lines(left, right)))
    }
}

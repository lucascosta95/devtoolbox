package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.Curl
import dev.devtoolbox.core.util.Diff
import dev.devtoolbox.core.util.Yaml
import dev.devtoolbox.core.util.YamlParseException

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

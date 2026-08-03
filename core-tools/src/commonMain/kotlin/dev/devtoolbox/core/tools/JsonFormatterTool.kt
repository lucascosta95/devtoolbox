package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.Json
import dev.devtoolbox.core.util.JsonParseException

object JsonFormatterTool : Tool {
    override val id = "json"
    override val name = "JSON Formatter"
    override val category = Category.Formatters
    override val icon = "brackets-curly"
    override val description = "Formate e indente JSON compactado para leitura."
    override val defaultInput = ToolInput.Text(
        """{"name":"Ana Souza","role":"Backend Developer","active":true,""" +
            """"skills":["Kotlin","Ktor","Postgres"],"team":{"name":"Platform","size":6}}""",
    )

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isBlank()) {
            return ToolOutput.Success(ToolBody.Io(text, "", "JSON", "JSON formatado"))
        }
        return try {
            ToolOutput.Success(ToolBody.Io(text, Json.format(text), "JSON", "JSON formatado"))
        } catch (e: JsonParseException) {
            ToolOutput.Failure(e.message ?: "JSON inválido.")
        }
    }
}

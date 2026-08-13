package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.JsonParseException
import dev.devtoolbox.core.util.JsonString

private const val INPUT_LABEL = "String JSON"

private const val OUTPUT_LABEL = "Objeto formatado"

object JsonStringParserTool : Tool {
    override val id = "json-string"
    override val name = "JSON String Parser"
    override val category = Category.Formatters
    override val icon = "quotes"
    override val description =
        "Cole um JSON serializado como string, com escapes, e veja o objeto formatado."
    override val defaultInput = ToolInput.Text(
        "\"" +
            """{\"orderId\":\"A-8842\",\"status\":\"SHIPPED\",""" +
            """\"items\":[{\"sku\":\"KT-01\",\"qty\":2}],\"total\":149.9}""" +
            "\"",
    )

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isBlank()) {
            return ToolOutput.Success(ToolBody.Io(text, "", INPUT_LABEL, OUTPUT_LABEL))
        }
        return try {
            ToolOutput.Success(
                ToolBody.Io(text, JsonString.format(text), INPUT_LABEL, OUTPUT_LABEL),
            )
        } catch (e: JsonParseException) {
            val message = e.message ?: "JSON inválido."
            ToolOutput.Failure(
                message,
                ToolBody.Io(text, "Entrada inválida: $message", INPUT_LABEL, OUTPUT_LABEL),
            )
        }
    }
}

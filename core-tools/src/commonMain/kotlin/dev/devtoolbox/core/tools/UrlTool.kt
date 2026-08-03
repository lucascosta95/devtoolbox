package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Direction
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.PercentEncoding

object UrlTool : Tool {
    override val id = "url"
    override val name = "URL Encode/Decode"
    override val category = Category.Encoding
    override val icon = "link-simple"
    override val description = "Escape ou desfaça o escape de caracteres especiais em URLs."
    override val defaultInput =
        ToolInput.Text("https://devtoolbox.dev/search?q=kotlin multiplatform&lang=pt-BR")

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isEmpty()) {
            return ToolOutput.Success(ToolBody.Io(text, ""))
        }

        val decoding = when (input.direction) {
            Direction.Encode -> false
            Direction.Decode -> true
            Direction.Auto -> PercentEncoding.looksEncoded(text)
        }

        return if (decoding) {
            try {
                ToolOutput.Success(
                    ToolBody.Io(text, PercentEncoding.decode(text), "URL codificada", "URL decodificada"),
                )
            } catch (e: PercentEncoding.DecodeException) {
                ToolOutput.Failure("URL inválida: ${e.message}")
            }
        } else {
            ToolOutput.Success(
                ToolBody.Io(text, PercentEncoding.encode(text), "URL", "URL codificada"),
            )
        }
    }
}

package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Direction
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.Base64

object Base64Tool : Tool {
    override val id = "base64"
    override val name = "Base64 Encode/Decode"
    override val category = Category.Encoding
    override val icon = "arrows-left-right"
    override val description = "Codifique e decodifique texto em Base64 instantaneamente."
    override val defaultInput = ToolInput.Text("Hello, World!")

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isEmpty()) {
            return ToolOutput.Success(ToolBody.Io(text, "", outputLabel = "Saída"))
        }

        val decoding = when (input.direction) {
            Direction.Encode -> false
            Direction.Decode -> true
            Direction.Auto -> Base64.looksLikeBase64(text)
        }

        return if (decoding) {
            try {
                ToolOutput.Success(
                    ToolBody.Io(text, Base64.decode(text), "Base64", "Texto decodificado"),
                )
            } catch (e: Base64.DecodeException) {
                ToolOutput.Failure("Base64 inválido: ${e.message}")
            }
        } else {
            ToolOutput.Success(
                ToolBody.Io(text, Base64.encode(text), "Texto", "Base64"),
            )
        }
    }
}

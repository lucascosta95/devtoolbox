package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Row
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.Digest

object HashTool : Tool {
    override val id = "hash"
    override val name = "Hash Generator"
    override val category = Category.Encoding
    override val icon = "fingerprint"
    override val description = "Gere hashes MD5, SHA-1 e SHA-256 a partir de um texto."
    override val defaultInput = ToolInput.Text("DevToolbox")

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        return ToolOutput.Success(
            ToolBody.Rows(
                listOf(
                    Row("Entrada", text),
                    Row("MD5", Digest.md5(text)),
                    Row("SHA-1", Digest.sha1(text)),
                    Row("SHA-256", Digest.sha256(text)),
                ),
            ),
        )
    }
}

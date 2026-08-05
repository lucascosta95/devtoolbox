package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.Base64
import dev.devtoolbox.core.util.Json
import dev.devtoolbox.core.util.JsonParseException

object JwtTool : Tool {
    override val id = "jwt"
    override val name = "JWT Decoder"
    override val category = Category.Encoding
    override val icon = "key"
    override val description = "Inspecione o header e o payload de um JSON Web Token."
    override val defaultInput = ToolInput.Text(
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
            "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
    )

    override fun run(input: ToolInput): ToolOutput {
        val token = (input as? ToolInput.Text)?.value?.trim()
            ?: return ToolOutput.Failure("Entrada inválida.")
        if (token.isEmpty()) return ToolOutput.Failure("Informe um token JWT.")

        val parts = token.split(".")
        if (parts.size != 3) {
            return ToolOutput.Failure(
                "JWT deve ter 3 partes separadas por ponto — recebidas ${parts.size}.",
            )
        }
        val (headerPart, payloadPart, signaturePart) = parts

        val headerJson = decodeSegment(headerPart, "header")
            ?: return ToolOutput.Failure("Header não é Base64URL válido.")
        val payloadJson = decodeSegment(payloadPart, "payload")
            ?: return ToolOutput.Failure("Payload não é Base64URL válido.")

        val headerPretty = prettyOrNull(headerJson)
            ?: return ToolOutput.Failure("Header decodificado não é um JSON válido.")
        val payloadPretty = prettyOrNull(payloadJson)
            ?: return ToolOutput.Failure("Payload decodificado não é um JSON válido.")

        return ToolOutput.Success(
            ToolBody.Jwt(
                headerPart = headerPart,
                payloadPart = payloadPart,
                signaturePart = signaturePart,
                headerJson = headerPretty,
                payloadJson = payloadPretty,
            ),
        )
    }

    private fun decodeSegment(segment: String, @Suppress("UNUSED_PARAMETER") name: String): String? =
        runCatching { Base64.decode(segment) }.getOrNull()

    private fun prettyOrNull(json: String): String? =
        try {
            Json.format(json)
        } catch (_: JsonParseException) {
            null
        }
}

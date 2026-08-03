package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.ImageDetails
import dev.devtoolbox.core.ImageSelection
import dev.devtoolbox.core.Row
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.EncodedImage
import dev.devtoolbox.core.util.ImageEncoder

/**
 * Imagem → data URI Base64.
 *
 * A ferramenta em si é barata: quem lê o arquivo e codifica é a camada de UI, fora da thread
 * principal (ver `ImagePicker`), e entrega o resultado pronto em [ImageSelection.Loaded].
 * `run` só monta as linhas — mantendo a promessa de que todo [Tool.run] é puro e instantâneo.
 */
object ImageBase64Tool : Tool {
    override val id = "img64"
    override val name = "Imagem → Base64"
    override val category = Category.Encoding
    override val icon = "image"
    override val description =
        "Converta uma imagem em uma data URI Base64 para embutir em HTML, CSS ou JSON."
    override val defaultInput = ToolInput.Image()

    override fun run(input: ToolInput): ToolOutput {
        val selection = (input as? ToolInput.Image)?.selection
            ?: return ToolOutput.Failure("Entrada inválida.")

        return when (selection) {
            is ImageSelection.Empty -> ToolOutput.Success(ToolBody.Image())
            is ImageSelection.Loading ->
                ToolOutput.Success(ToolBody.Image(loading = true))
            // O corpo vazio mantém a área de arrastar na tela: o erro não pode tirar do
            // usuário o caminho para tentar outro arquivo.
            is ImageSelection.Failed ->
                ToolOutput.Failure(selection.message, ToolBody.Image())
            is ImageSelection.Loaded -> ToolOutput.Success(
                ToolBody.Image(details = detailsOf(selection.image), source = selection.image),
            )
        }
    }

    private fun detailsOf(image: EncodedImage): ImageDetails {
        val growth = ImageEncoder.growthPercent(image.originalBytes, image.base64Bytes)
        return ImageDetails(
            rows = listOf(
                Row("Arquivo", image.fileName),
                Row("Dimensões", image.dimensions ?: "não informadas"),
                Row("Tipo MIME", image.mime),
                Row("Tamanho original", ImageEncoder.formatBytes(image.originalBytes)),
                Row(
                    "Tamanho em Base64",
                    "${ImageEncoder.formatBytes(image.base64Bytes)} (+$growth%)",
                ),
            ),
            dataUri = image.dataUri,
            snippets = listOf(
                Row("HTML", ImageEncoder.htmlSnippet(image)),
                Row("CSS", ImageEncoder.cssSnippet(image)),
            ),
        )
    }
}

package dev.devtoolbox.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.devtoolbox.core.ImageSelection
import dev.devtoolbox.core.util.ImageEncodeResult
import dev.devtoolbox.core.util.ImageEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Arquivo escolhido pelo usuário.
 *
 * [read] é suspensa de propósito: o nome aparece na UI (estado "carregando") **antes** de os
 * bytes serem lidos, e a leitura acontece fora da thread principal.
 */
class PickedFile(val name: String, val read: suspend () -> ByteArray)

/** Abre o seletor de arquivos do sistema. `null` quando o usuário cancela. */
expect suspend fun pickImageFile(): PickedFile?

/**
 * Alvo de arrastar-e-soltar para imagens vindas **de fora** da aplicação.
 *
 * O gesto é da plataforma (AWT no desktop), por isso a implementação é `actual`.
 */
@Composable
expect fun Modifier.imageDropTarget(
    onDragStateChange: (Boolean) -> Unit,
    onFileDropped: (PickedFile) -> Unit,
): Modifier

/** Dispatcher de trabalho da plataforma — `Dispatchers.Default` em todas as atuais. */
expect val ioDispatcher: CoroutineDispatcher

/**
 * Lê e codifica o arquivo fora da thread de UI.
 *
 * Um PNG de 5 MB vira ~6,8 MB de Base64; fazer isso na thread principal congelaria a janela
 * por bons décimos de segundo, então o trabalho todo mora aqui.
 */
suspend fun loadImageSelection(file: PickedFile): ImageSelection = withContext(ioDispatcher) {
    val bytes = runCatching { file.read() }.getOrElse { error ->
        return@withContext ImageSelection.Failed(
            file.name,
            "Não foi possível ler o arquivo: ${error.message ?: "erro desconhecido"}",
        )
    }
    when (val result = ImageEncoder.encode(file.name, bytes)) {
        is ImageEncodeResult.Ok -> ImageSelection.Loaded(result.image)
        is ImageEncodeResult.Error -> ImageSelection.Failed(file.name, result.message)
    }
}

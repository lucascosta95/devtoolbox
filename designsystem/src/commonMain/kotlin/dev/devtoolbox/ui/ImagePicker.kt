package dev.devtoolbox.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import dev.devtoolbox.core.ImageSelection
import dev.devtoolbox.core.util.EncodedImage
import dev.devtoolbox.core.util.ImageEncodeResult
import dev.devtoolbox.core.util.ImageEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class PickedFile(val name: String, val read: suspend () -> ByteArray)

expect suspend fun pickImageFile(): PickedFile?

@Composable
expect fun Modifier.imageDropTarget(
    onDragStateChange: (Boolean) -> Unit,
    onFileDropped: (PickedFile) -> Unit,
): Modifier

expect val ioDispatcher: CoroutineDispatcher

expect fun decodeImage(image: EncodedImage, density: Density): Painter?

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

package dev.devtoolbox.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.unit.Density
import dev.devtoolbox.core.util.EncodedImage
import dev.devtoolbox.core.util.ImageFormat
import org.jetbrains.skia.Image
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.SwingUtilities

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual suspend fun pickImageFile(): PickedFile? {
    val chosen = awaitFileDialog() ?: return null
    return chosen.asPickedFile()
}

private suspend fun awaitFileDialog(): File? {
    val result = CompletableDeferred<File?>()
    SwingUtilities.invokeLater {
        runCatching {
            val dialog = FileDialog(null as Frame?, "Escolha uma imagem", FileDialog.LOAD)
            dialog.filenameFilter = java.io.FilenameFilter { _, name -> name.hasImageExtension() }
            dialog.isVisible = true
            result.complete(dialog.files.firstOrNull())
        }.onFailure { result.complete(null) }
    }
    return result.await()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.imageDropTarget(
    onDragStateChange: (Boolean) -> Unit,
    onFileDropped: (PickedFile) -> Unit,
): Modifier {
    val target = remember(onDragStateChange, onFileDropped) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = onDragStateChange(true)

            override fun onExited(event: DragAndDropEvent) = onDragStateChange(false)

            override fun onEnded(event: DragAndDropEvent) = onDragStateChange(false)

            override fun onDrop(event: DragAndDropEvent): Boolean {
                onDragStateChange(false)
                val file = event.droppedFiles().firstOrNull { it.name.hasImageExtension() }
                    ?: return false
                onFileDropped(file.asPickedFile())
                return true
            }
        }
    }

    return dragAndDropTarget(
        shouldStartDragAndDrop = { event -> event.hasFiles() },
        target = target,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun DragAndDropEvent.hasFiles(): Boolean =
    runCatching { awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) }
        .getOrDefault(false)

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalComposeUiApi::class)
private fun DragAndDropEvent.droppedFiles(): List<File> = runCatching {
    if (!awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return emptyList()
    (awtTransferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>)
}.getOrDefault(emptyList())

private fun File.asPickedFile() = PickedFile(name) { withContext(Dispatchers.IO) { readBytes() } }

private fun String.hasImageExtension(): Boolean =
    substringAfterLast('.', "").lowercase() in ImageFormat.allExtensions

actual fun decodeImage(image: EncodedImage, density: Density): Painter? = runCatching {
    if (image.format == ImageFormat.Svg) {
        loadSvgPainter(image.bytes.inputStream(), density)
    } else {
        BitmapPainter(Image.makeFromEncoded(image.bytes).toComposeImageBitmap())
    }
}.getOrNull()

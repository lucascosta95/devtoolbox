package dev.devtoolbox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.NocturneTheme
import dev.devtoolbox.ds.Palettes
import dev.devtoolbox.ds.Ramp
import dev.devtoolbox.ds.components.PhosphorIcon
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage

@OptIn(ExperimentalComposeUiApi::class)
fun renderAppIcon(size: Int): Image {
    val scene = ImageComposeScene(width = size, height = size, density = Density(1f)) {
        NocturneTheme(Palettes.NocturneDark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape((size * 0.1875f).dp))
                    .background(Ramp.accent900),
                contentAlignment = Alignment.Center,
            ) {
                PhosphorIcon("wrench", size = (size * 0.547f).dp, tint = Ramp.accent300)
            }
        }
    }
    scene.render(0)
    val image = scene.render(600_000_000L)
    scene.close()
    return image
}

// Sem ícone explícito, o AWT mostra a xícara do Java na barra de título (Windows) e no
// alt-tab. Vários tamanhos deixam o sistema escolher o mais nítido para cada escala de DPI.
fun appWindowIcons(): List<BufferedImage> =
    listOf(16, 20, 24, 32, 40, 48, 64, 128, 256)
        .map { renderAppIcon(it).toComposeImageBitmap().toAwtImage() }

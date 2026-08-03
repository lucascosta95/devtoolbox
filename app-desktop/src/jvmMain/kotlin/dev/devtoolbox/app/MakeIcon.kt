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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.NocturneTheme
import dev.devtoolbox.ds.Ramp
import dev.devtoolbox.ds.ThemeMode
import dev.devtoolbox.ds.components.PhosphorIcon
import org.jetbrains.skia.EncodedImageFormat
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Gera os ícones da aplicação a partir do próprio design system — a chave inglesa do Phosphor
 * sobre o fundo accent, com os mesmos tokens da UI.
 *
 * Emite os três formatos que o `jpackage` espera, um por plataforma:
 * `devtoolbox.png` (Linux), `devtoolbox.ico` (Windows) e `devtoolbox.icns` (macOS).
 *
 * `.ico` e `.icns` são escritos à mão porque são apenas contêineres de PNG — assim o build
 * não depende de ImageMagick, `iconutil` ou qualquer ferramenta externa, e roda igual nos
 * três runners do CI.
 *
 * Uso: `./gradlew :app-desktop:appIcon`.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val outDir = File(args.getOrElse(0) { "src/jvmMain/resources/icons" }).apply { mkdirs() }

    val png512 = renderPng(512)
    File(outDir, "devtoolbox.png").writeBytes(png512)

    // Windows: tamanhos que o Explorer usa em lista, detalhes e ladrilhos.
    val icoSizes = listOf(16, 32, 48, 64, 128, 256)
    File(outDir, "devtoolbox.ico").writeBytes(buildIco(icoSizes.map { it to renderPng(it) }))

    // macOS: os tipos que o Finder e o Dock consultam, incluindo as variantes @2x.
    val icnsEntries = listOf(
        "ic11" to 32, "ic12" to 64, "ic07" to 128,
        "ic13" to 256, "ic08" to 256, "ic14" to 512, "ic09" to 512,
    )
    File(outDir, "devtoolbox.icns").writeBytes(buildIcns(icnsEntries.map { (t, s) -> t to renderPng(s) }))

    println("ícones escritos em ${outDir.absolutePath}: devtoolbox.png, .ico, .icns")
}

/** Desenha o ícone no tamanho pedido e devolve os bytes do PNG. */
@OptIn(ExperimentalComposeUiApi::class)
private fun renderPng(size: Int): ByteArray {
    val scene = ImageComposeScene(width = size, height = size, density = Density(1f)) {
        NocturneTheme(ThemeMode.Dark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Proporções fixas em relação ao lado: o ícone fica igual em qualquer tamanho.
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
    val bytes = image.encodeToData(EncodedImageFormat.PNG)?.bytes
        ?: error("falha ao codificar o PNG de ${size}px")
    scene.close()
    return bytes
}

/**
 * Formato ICO: um cabeçalho, um diretório de entradas e os PNGs em sequência.
 * Entradas com PNG embutido são suportadas do Windows Vista em diante.
 */
private fun buildIco(images: List<Pair<Int, ByteArray>>): ByteArray {
    val out = ByteArrayOutputStream()

    fun le16(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
    fun le32(v: Int) {
        out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
        out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF)
    }

    le16(0)                 // reservado
    le16(1)                 // tipo 1 = ícone
    le16(images.size)

    var offset = 6 + images.size * 16
    for ((size, png) in images) {
        // 0 representa 256 no campo de um byte.
        out.write(if (size >= 256) 0 else size)
        out.write(if (size >= 256) 0 else size)
        out.write(0)        // paleta
        out.write(0)        // reservado
        le16(1)             // planos
        le16(32)            // bits por pixel
        le32(png.size)
        le32(offset)
        offset += png.size
    }
    for ((_, png) in images) out.write(png)
    return out.toByteArray()
}

/**
 * Formato ICNS: magic `icns`, tamanho total, e uma sequência de blocos
 * `[tipo(4)][tamanho(4)][dados]` — com PNG cru nos tipos `ic07`+.
 */
private fun buildIcns(entries: List<Pair<String, ByteArray>>): ByteArray {
    val body = ByteArrayOutputStream()

    fun be32(stream: ByteArrayOutputStream, v: Int) {
        stream.write((v shr 24) and 0xFF); stream.write((v shr 16) and 0xFF)
        stream.write((v shr 8) and 0xFF); stream.write(v and 0xFF)
    }

    for ((type, png) in entries) {
        body.write(type.encodeToByteArray())
        be32(body, png.size + 8) // o tamanho do bloco inclui o próprio cabeçalho
        body.write(png)
    }

    val out = ByteArrayOutputStream()
    out.write("icns".encodeToByteArray())
    be32(out, body.size() + 8)
    out.write(body.toByteArray())
    return out.toByteArray()
}

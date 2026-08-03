package dev.devtoolbox.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolRegistry
import dev.devtoolbox.ds.ThemeMode
import dev.devtoolbox.ui.App
import dev.devtoolbox.ui.AppState
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Renderiza estados da janela para PNG **sem abrir display** — usado para conferir a UI
 * contra o protótipo (`design/DevToolbox.dc.html`).
 *
 * Uso: `./gradlew :app-desktop:screenshot`
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val outDir = File(args.getOrElse(0) { "build/screenshots" }).apply { mkdirs() }

    // Uma tela por ferramenta, mais os estados da moldura.
    val shots = ToolRegistry.all.map { it.id to AppState(selectedId = it.id) } + listOf(
        "tema-light" to AppState(theme = ThemeMode.Light),
        "cnpj-light" to AppState(selectedId = "cnpj", theme = ThemeMode.Light),
        "busca" to AppState(query = "enc"),
        "busca-validadores" to AppState(query = "valida", selectedId = "card"),
        "img64-carregado" to AppState(
            selectedId = "img64",
            inputs = mapOf("img64" to dev.devtoolbox.core.ToolInput.Image(sampleImage())),
        ),
        "rodape" to AppState(query = "qr"),
        "busca-vazia" to AppState(query = "zzz"),
        "accent-teal" to AppState(selectedId = "card", accent = dev.devtoolbox.ds.AccentColor.Teal),
        "accent-amber-light" to AppState(
            selectedId = "regex",
            theme = ThemeMode.Light,
            accent = dev.devtoolbox.ds.AccentColor.Amber,
        ),
        "erro-json" to AppState(
            selectedId = "json",
            inputs = mapOf("json" to ToolInput.Text("""{"a": 1, "b" 2}""")),
        ),
        "erro-jwt" to AppState(
            selectedId = "jwt",
            inputs = mapOf("jwt" to ToolInput.Text("abc.def")),
        ),
        "erro-cron" to AppState(
            selectedId = "cron",
            inputs = mapOf("cron" to ToolInput.Text("0 9 L * *")),
        ),
    )

    for ((name, state) in shots) {
        val scene = ImageComposeScene(width = 1180, height = 740, density = Density(1f)) {
            App(initialState = state)
        }
        // Dois frames: o primeiro dispara o carregamento assíncrono de fontes e SVGs.
        scene.render(0)
        val image = scene.render(600_000_000L)
        File(outDir, "$name.png").also { file ->
            image.encodeToData(EncodedImageFormat.PNG)?.bytes?.let(file::writeBytes)
            println("escrito: ${file.name}")
        }
        scene.close()
    }
}

/**
 * Um PNG de verdade (8×8, cinza) só para a screenshot do arquétipo `image` — o encoder do core
 * é exercitado pelos testes, aqui basta ter bytes válidos.
 */
private fun sampleImage(): dev.devtoolbox.core.ImageSelection {
    val image = java.awt.image.BufferedImage(320, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.color = java.awt.Color(0x91, 0x84, 0xD9)
    g.fillRect(0, 0, 320, 200)
    g.dispose()
    val bytes = java.io.ByteArrayOutputStream()
        .also { javax.imageio.ImageIO.write(image, "png", it) }
        .toByteArray()

    val result = dev.devtoolbox.core.util.ImageEncoder.encode("logo-devtoolbox.png", bytes)
    return dev.devtoolbox.core.ImageSelection.Loaded(
        (result as dev.devtoolbox.core.util.ImageEncodeResult.Ok).image,
    )
}

package dev.devtoolbox.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolRegistry
import dev.devtoolbox.ds.ThemeMode
import dev.devtoolbox.ui.App
import dev.devtoolbox.ui.AppState
import dev.devtoolbox.ui.panels.MIN_IO_PANEL_HEIGHT
import dev.devtoolbox.ui.panels.PanelStore
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val outDir = File(args.getOrElse(0) { "build/screenshots" }).apply { mkdirs() }

    val shots = ToolRegistry.all.map { Shot(it.id, AppState(selectedId = it.id)) } + listOf(
        "tema-light" to AppState(theme = ThemeMode.Light),
        "cnpj-light" to AppState(selectedId = "cnpj", theme = ThemeMode.Light),
        "busca" to AppState(query = "enc"),
        "busca-validadores" to AppState(query = "valida", selectedId = "card"),
        "img64-carregado" to AppState(
            selectedId = "img64",
            inputs = mapOf("img64" to dev.devtoolbox.core.ToolInput.Image(sampleImage())),
        ),
        "rodape" to AppState(query = "qr"),
        "img64-transparencia" to AppState(
            selectedId = "img64",
            inputs = mapOf("img64" to dev.devtoolbox.core.ToolInput.Image(sampleImage(alpha = true))),
        ),
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
        "erro-json-string" to AppState(
            selectedId = "json-string",
            inputs = mapOf("json-string" to ToolInput.Text("\"{\\\"a\\\" 1}\"")),
        ),
        "substring-vazio" to AppState(
            selectedId = "substring",
            inputs = mapOf(
                "substring" to ToolInput.Slice("DevToolbox", "6", "2"),
            ),
        ),
    ).map { (name, state) -> Shot(name, state) } + listOf(
        Shot(
            name = "painel-redimensionado",
            state = AppState(selectedId = "json"),
            panels = PanelStore().apply {
                block("json-io", 260.dp, MIN_IO_PANEL_HEIGHT).resizeTo(430.dp)
            },
        ),
        Shot(
            name = "divisor-fora-do-centro",
            state = AppState(selectedId = "base64"),
            panels = PanelStore().apply {
                block("base64-io", 260.dp, MIN_IO_PANEL_HEIGHT).splitTo(0.3f)
            },
        ),
        Shot(
            name = "coluna-alargada",
            state = AppState(selectedId = "json"),
            panels = PanelStore().apply { content.resizeTo(2_000.dp) },
        ),
        Shot(
            name = "jwt-divisor-e-altura",
            state = AppState(selectedId = "jwt"),
            panels = PanelStore().apply {
                block("jwt-decoded", 200.dp).apply {
                    resizeTo(300.dp)
                    splitTo(0.68f)
                }
            },
        ),
    )

    for (shot in shots) {
        val scene = ImageComposeScene(width = 1180, height = 740, density = Density(1f)) {
            App(initialState = shot.state, panels = shot.panels)
        }
        scene.render(0)
        val image = scene.render(600_000_000L)
        File(outDir, "${shot.name}.png").also { file ->
            image.encodeToData(EncodedImageFormat.PNG)?.bytes?.let(file::writeBytes)
            println("escrito: ${file.name}")
        }
        scene.close()
    }
}

private class Shot(val name: String, val state: AppState, val panels: PanelStore = PanelStore())

private fun sampleImage(alpha: Boolean = false): dev.devtoolbox.core.ImageSelection {
    val image = java.awt.image.BufferedImage(320, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    if (alpha) {
        g.color = java.awt.Color(0xD9, 0xA5, 0x44)
        g.fillOval(60, 20, 160, 160)
    } else {
        g.color = java.awt.Color(0x91, 0x84, 0xD9)
        g.fillRect(0, 0, 320, 200)
    }
    g.dispose()
    val bytes = java.io.ByteArrayOutputStream()
        .also { javax.imageio.ImageIO.write(image, "png", it) }
        .toByteArray()

    val result = dev.devtoolbox.core.util.ImageEncoder.encode("logo-devtoolbox.png", bytes)
    return dev.devtoolbox.core.ImageSelection.Loaded(
        (result as dev.devtoolbox.core.util.ImageEncodeResult.Ok).image,
    )
}

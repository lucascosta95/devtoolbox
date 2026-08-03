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
        "busca-vazia" to AppState(query = "zzz"),
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
        val scene = ImageComposeScene(width = 1440, height = 900, density = Density(1f)) {
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

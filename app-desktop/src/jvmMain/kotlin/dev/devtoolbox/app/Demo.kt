package dev.devtoolbox.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.update.Release
import dev.devtoolbox.ds.AccentColor
import dev.devtoolbox.ds.ThemeMode
import dev.devtoolbox.ui.App
import dev.devtoolbox.ui.AppState
import dev.devtoolbox.ui.UpdateNotice
import dev.devtoolbox.ui.panels.PanelStore
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.util.Locale

private class Step(val state: AppState, val seconds: Double)

private const val BEAT = 1.5

private const val HERO = 2.0

private class Board {
    val steps = mutableListOf<Step>()

    fun hold(state: AppState, seconds: Double = BEAT) {
        steps += Step(state, seconds)
    }

    fun typeText(
        base: AppState,
        toolId: String,
        text: String,
        perStep: Int = 2,
        stepSeconds: Double = 0.07,
        from: Int = 1,
    ) {
        var size = from
        while (size < text.length) {
            hold(base.withText(toolId, text.take(size)), stepSeconds)
            size += perStep
        }
    }

    fun typeQuery(base: AppState, query: String, stepSeconds: Double = 0.11) {
        for (size in 1..query.length) {
            hold(base.copy(query = query.take(size)), stepSeconds)
        }
    }

    fun typePattern(
        base: AppState,
        pattern: String,
        flags: String,
        subject: String,
        stepSeconds: Double = 0.08,
    ) {
        for (size in 1 until pattern.length) {
            hold(base.withPattern(pattern.take(size), flags, subject), stepSeconds)
        }
    }
}

private fun AppState.withText(toolId: String, value: String): AppState =
    copy(selectedId = toolId, inputs = inputs + (toolId to ToolInput.Text(value)))

private fun AppState.withPattern(pattern: String, flags: String, subject: String): AppState =
    copy(
        selectedId = "regex",
        inputs = inputs + ("regex" to ToolInput.Pattern(pattern, flags, subject)),
    )

private fun AppState.withSeed(toolId: String, nonce: Int): AppState =
    copy(selectedId = toolId, inputs = inputs + (toolId to ToolInput.Seed(nonce)))

private fun AppState.withImage(): AppState =
    copy(
        selectedId = "img64",
        inputs = inputs + ("img64" to ToolInput.Image(sampleImage())),
    )

private fun AppState.on(toolId: String): AppState = copy(selectedId = toolId)

private const val BASE64_LINE = "Caixa de ferramentas offline"

private const val JSON_LINE =
    """{"user":{"id":42,"name":"Ana","roles":["admin","dev"]},"active":true}"""

private const val URL_LINE = "https://api.devtoolbox.dev/busca?q=café com leite&pagina=2"

private const val HASH_LINE = "DevToolbox"

private const val CASE_LINE = "caixa de ferramentas offline"

private const val COLOR_LINE = "#5cb5a3"

private const val CRON_LINE = "0 9 * * 1-5"

private const val QR_LINE = "https://github.com/lucascosta95/devtoolbox"

private const val CPF_LINE = "529.982.247-25"

private const val REGEX_PATTERN = """(\d{3})-(\d{4})"""

private const val REGEX_SUBJECT = "Suporte: 555-0132 ou 555-0148."

private fun storyboard(): List<Step> = Board().apply {
    var s = AppState(selectedId = "base64")

    typeText(s, "base64", BASE64_LINE, perStep = 2, stepSeconds = 0.07)
    s = s.withText("base64", BASE64_LINE)
    hold(s, HERO)

    typeQuery(s, "json")
    hold(s.copy(query = "json"), 1.2)

    s = s.copy(query = "").on("json").withText("json", "")
    hold(s.withText("json", JSON_LINE.take(1)), 0.3)
    typeText(s, "json", JSON_LINE, perStep = 4, stepSeconds = 0.06, from = 2)
    s = s.withText("json", JSON_LINE)
    hold(s, HERO)

    hold(s.on("json-string"))
    hold(s.on("yaml"))
    hold(s.on("curl"), 1.8)
    hold(s.on("sql"), 1.8)
    hold(s.on("nrql"))
    hold(s.on("diff"), HERO)
    hold(s.on("jwt"), HERO)

    s = s.on("url").withText("url", "")
    typeText(s, "url", URL_LINE, perStep = 4, stepSeconds = 0.06)
    s = s.withText("url", URL_LINE)
    hold(s, HERO)

    typeText(s.withText("hash", ""), "hash", HASH_LINE, perStep = 2, stepSeconds = 0.08)
    s = s.withText("hash", HASH_LINE)
    hold(s, HERO)

    s = s.withImage()
    hold(s, HERO)

    typeText(s.withText("string-case", ""), "string-case", CASE_LINE, perStep = 3, stepSeconds = 0.06)
    s = s.withText("string-case", CASE_LINE)
    hold(s, BEAT)

    hold(s.on("substring"))
    hold(s.on("lorem"))

    hold(s.withSeed("uuid", 0), 1.3)
    hold(s.withSeed("uuid", 1), 1.3)
    s = s.withSeed("uuid", 2)
    hold(s, 1.5)

    s = s.on("regex")
    typePattern(s, REGEX_PATTERN, "g", REGEX_SUBJECT)
    s = s.withPattern(REGEX_PATTERN, "g", REGEX_SUBJECT)
    hold(s, HERO)

    typeText(s.withText("color", ""), "color", COLOR_LINE, perStep = 1, stepSeconds = 0.09)
    s = s.withText("color", COLOR_LINE)
    hold(s, BEAT)

    hold(s.on("timestamp"))

    typeText(s.withText("cron", ""), "cron", CRON_LINE, perStep = 2, stepSeconds = 0.08)
    s = s.withText("cron", CRON_LINE)
    hold(s, 1.8)

    s = s.on("qr").withText("qr", "")
    typeText(s, "qr", QR_LINE, perStep = 3, stepSeconds = 0.05)
    s = s.withText("qr", QR_LINE)
    hold(s, HERO)

    typeText(s.withText("cpf", ""), "cpf", CPF_LINE, perStep = 1, stepSeconds = 0.08)
    s = s.withText("cpf", CPF_LINE)
    hold(s, 2.0)

    hold(s.on("cnpj"), 1.5)
    hold(s.on("card"), 1.5)
    hold(s.on("phone"), 1.5)

    s = s.on("json")
    hold(
        s.copy(
            updateNotice = UpdateNotice.Available(
                Release(
                    tag = "v1.5.1",
                    url = "https://github.com/lucascosta95/devtoolbox/releases/latest",
                ),
            ),
        ),
        HERO,
    )

    hold(s.copy(theme = ThemeMode.Light), BEAT)
    hold(s.copy(theme = ThemeMode.Light, accent = AccentColor.Cyan), 0.5)
    hold(s.copy(theme = ThemeMode.Light, accent = AccentColor.Lime), 0.5)
    hold(s.copy(theme = ThemeMode.Light, accent = AccentColor.Amber), 0.5)
    hold(s.copy(accent = AccentColor.Rose), 0.5)
    hold(s.copy(accent = AccentColor.Teal), 0.5)

    hold(s.on("base64"), 2.4)
}.steps

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val outDir = File(args.getOrElse(0) { "build/demo" }).apply { mkdirs() }
    outDir.listFiles()?.filter { it.name.endsWith(".png") || it.name == "plan.txt" }?.forEach(File::delete)

    val steps = storyboard()
    val plan = StringBuilder("ffconcat version 1.0\n")

    steps.forEachIndexed { index, step ->
        val scene = ImageComposeScene(width = 1180, height = 740, density = Density(1f)) {
            App(initialState = step.state, panels = PanelStore())
        }
        scene.render(0)
        val image = scene.render(600_000_000L)
        val name = "f%04d.png".format(index)
        File(outDir, name).also { file ->
            image.encodeToData(EncodedImageFormat.PNG)?.bytes?.let(file::writeBytes)
        }
        scene.close()
        plan.append("file '").append(name).append("'\n")
        plan.append("duration ").append("%.3f".format(Locale.US, step.seconds)).append('\n')
        if (index == steps.lastIndex) plan.append("file '").append(name).append("'\n")
    }

    File(outDir, "plan.txt").writeText(plan.toString())

    println("quadros: ${steps.size}")
    println("duração: %.1fs".format(Locale.US, steps.sumOf { it.seconds }))
}

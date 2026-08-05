package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Row
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.ColorConvert
import dev.devtoolbox.core.util.ColorParseException
import dev.devtoolbox.core.util.Cron
import dev.devtoolbox.core.util.CronParseException
import dev.devtoolbox.core.util.QrCode
import dev.devtoolbox.core.util.QrEncodeException
import dev.devtoolbox.core.util.Uuid
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import dev.devtoolbox.core.util.isoNumber
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object UuidTool : Tool {
    override val id = "uuid"
    override val name = "UUID Generator"
    override val category = Category.Generators
    override val icon = "identification-badge"
    override val description = "Gere identificadores únicos universais (v4)."
    override val defaultInput = ToolInput.Seed(0)

    private const val COUNT = 3

    override fun run(input: ToolInput): ToolOutput {
        val seed = (input as? ToolInput.Seed)?.nonce ?: 0
        val uuids = Uuid.v4Batch(COUNT, seed)
        return ToolOutput.Success(
            ToolBody.Rows(
                rows = uuids.mapIndexed { i, u -> Row("UUID ${i + 1}", u) },
                regenerable = true,
            ),
        )
    }
}

object ColorTool : Tool {
    override val id = "color"
    override val name = "Color Converter"
    override val category = Category.Generators
    override val icon = "palette"
    override val description = "Converta cores entre HEX, RGB, HSL e OKLCH."
    override val defaultInput = ToolInput.Text("#9184d9")
    override val singleLineInput = true

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isBlank()) return ToolOutput.Failure("Informe uma cor.")
        val rgb = try {
            ColorConvert.parse(text)
        } catch (e: ColorParseException) {
            return ToolOutput.Failure("Cor inválida: ${e.message}")
        }
        val hex = ColorConvert.toHex(rgb)
        return ToolOutput.Success(
            ToolBody.Rows(
                listOf(
                    Row("HEX", hex, swatch = hex),
                    Row("RGB", ColorConvert.toRgbString(rgb)),
                    Row("HSL", ColorConvert.toHslString(rgb)),
                    Row("OKLCH", ColorConvert.toOklchString(rgb)),
                ),
            ),
        )
    }
}

@OptIn(ExperimentalTime::class)
object TimestampTool : Tool {
    override val id = "timestamp"
    override val name = "Timestamp Converter"
    override val category = Category.Generators
    override val icon = "clock-countdown"
    override val description = "Converta entre timestamp Unix e formatos de data legíveis."
    override val defaultInput = ToolInput.Text("1778580000")
    override val singleLineInput = true

    private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val MONTHS = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    var now: () -> Instant = { Clock.System.now() }
    var zone: () -> TimeZone = { TimeZone.currentSystemDefault() }

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value?.trim()
            ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isEmpty()) return ToolOutput.Failure("Informe um timestamp ou uma data ISO 8601.")

        val instant = parseInstant(text)
            ?: return ToolOutput.Failure(
                "Não reconheci '$text' — use Unix em segundos/milissegundos ou ISO 8601.",
            )

        val utc = instant.toLocalDateTime(TimeZone.UTC)
        val local = instant.toLocalDateTime(zone())

        return ToolOutput.Success(
            ToolBody.Rows(
                listOf(
                    Row("Unix (s)", instant.epochSeconds.toString()),
                    Row("Unix (ms)", instant.toEpochMilliseconds().toString()),
                    Row("ISO 8601", instant.toString()),
                    Row("UTC", rfc1123(utc)),
                    Row("Local (${zone().id})", brazilian(local)),
                    Row("Relativo", relative(instant)),
                ),
            ),
        )
    }

    private fun parseInstant(text: String): Instant? {
        text.toLongOrNull()?.let { number ->
            return if (abs(number) >= 100_000_000_000L) {
                Instant.fromEpochMilliseconds(number)
            } else {
                Instant.fromEpochSeconds(number)
            }
        }
        return runCatching { Instant.parse(text) }.getOrNull()
    }

    private fun rfc1123(dt: kotlinx.datetime.LocalDateTime): String {
        val weekday = WEEKDAYS[dt.dayOfWeek.isoNumber - 1]
        val month = MONTHS[dt.monthNumber - 1]
        return "$weekday, ${pad(dt.dayOfMonth)} $month ${dt.year} " +
            "${pad(dt.hour)}:${pad(dt.minute)}:${pad(dt.second)} GMT"
    }

    private fun brazilian(dt: kotlinx.datetime.LocalDateTime): String =
        "${pad(dt.dayOfMonth)}/${pad(dt.monthNumber)}/${dt.year} " +
            "${pad(dt.hour)}:${pad(dt.minute)}:${pad(dt.second)}"

    private fun relative(instant: Instant): String {
        val seconds = (now() - instant).inWholeSeconds
        val past = seconds >= 0
        val abs = abs(seconds)
        val (value, unit) = when {
            abs < 60 -> abs to "segundo"
            abs < 3600 -> abs / 60 to "minuto"
            abs < 86_400 -> abs / 3600 to "hora"
            abs < 2_592_000 -> abs / 86_400 to "dia"
            abs < 31_536_000 -> abs / 2_592_000 to "mês"
            else -> abs / 31_536_000 to "ano"
        }
        val plural = if (value == 1L) unit else if (unit == "mês") "meses" else "${unit}s"
        return if (past) "há $value $plural" else "em $value $plural"
    }

    private fun pad(v: Int) = v.toString().padStart(2, '0')
}

@OptIn(ExperimentalTime::class)
object CronTool : Tool {
    override val id = "cron"
    override val name = "Cron Parser"
    override val category = Category.Generators
    override val icon = "calendar-check"
    override val description = "Interprete e descreva expressões cron em linguagem natural."
    override val defaultInput = ToolInput.Text("0 9 * * 1-5")
    override val singleLineInput = true

    var now: () -> Instant = { Clock.System.now() }
    var zone: () -> TimeZone = { TimeZone.currentSystemDefault() }

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isBlank()) return ToolOutput.Failure("Informe uma expressão cron.")

        val cron = try {
            Cron.parse(text)
        } catch (e: CronParseException) {
            return ToolOutput.Failure("Cron inválido: ${e.message}")
        }

        val next = Cron.nextRun(cron, now().toLocalDateTime(zone()), zone())
        return ToolOutput.Success(
            ToolBody.Rows(
                listOf(
                    Row("Expressão", text.trim()),
                    Row("Descrição", Cron.describe(cron)),
                    Row("Minuto", Cron.describeField(cron.minute, "minute")),
                    Row("Hora", Cron.describeField(cron.hour, "hour")),
                    Row("Dia do mês", Cron.describeField(cron.dayOfMonth, "dom")),
                    Row("Mês", Cron.describeField(cron.month, "month")),
                    Row("Dia da semana", Cron.describeField(cron.dayOfWeek, "dow")),
                    Row(
                        "Próxima execução",
                        next?.let {
                            "${it.year}-${pad(it.monthNumber)}-${pad(it.dayOfMonth)} " +
                                "${pad(it.hour)}:${pad(it.minute)}"
                        } ?: "nenhuma nos próximos 12 meses",
                    ),
                ),
            ),
        )
    }

    private fun pad(v: Int) = v.toString().padStart(2, '0')
}

object QrTool : Tool {
    override val id = "qr"
    override val name = "QR Code Generator"
    override val category = Category.Generators
    override val icon = "qr-code"
    override val description = "Gere um QR Code a partir de um texto ou URL."
    override val defaultInput = ToolInput.Text("https://devtoolbox.dev")
    override val singleLineInput = true

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isEmpty()) return ToolOutput.Failure("Informe um texto ou URL.")
        return try {
            ToolOutput.Success(ToolBody.Qr(text, QrCode.encode(text)))
        } catch (e: QrEncodeException) {
            ToolOutput.Failure("Não foi possível gerar o QR: ${e.message}")
        }
    }
}

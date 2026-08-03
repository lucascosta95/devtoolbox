package dev.devtoolbox.core.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * Parser de expressões cron de **5 campos** (minuto, hora, dia do mês, mês, dia da semana).
 *
 * Suporta `*`, número, intervalo `a-b`, passo (`asterisco/n` e `a-b/n`) e listas por vírgula.
 * **Não** suporta segundos, `L`, `W`, `#` nem atalhos `@daily` — nesses casos o erro nomeia
 * o que não foi entendido.
 */
class CronParseException(message: String) : Exception(message)

data class CronField(val name: String, val expression: String, val values: Set<Int>)

data class CronExpression(
    val minute: CronField,
    val hour: CronField,
    val dayOfMonth: CronField,
    val month: CronField,
    val dayOfWeek: CronField,
) {
    val fields: List<CronField> get() = listOf(minute, hour, dayOfMonth, month, dayOfWeek)
}

object Cron {

    private val MONTH_NAMES = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )
    private val WEEKDAY_NAMES = listOf(
        "domingo", "segunda", "terça", "quarta", "quinta", "sexta", "sábado",
    )

    fun parse(expression: String): CronExpression {
        val text = expression.trim()
        if (text.startsWith("@")) {
            throw CronParseException("atalhos como '$text' não são suportados — use os 5 campos.")
        }
        val parts = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.size != 5) {
            throw CronParseException(
                "esperava 5 campos (minuto hora dia mês dia-da-semana) — recebidos ${parts.size}.",
            )
        }
        return CronExpression(
            minute = field("Minuto", parts[0], 0, 59),
            hour = field("Hora", parts[1], 0, 23),
            dayOfMonth = field("Dia do mês", parts[2], 1, 31),
            month = field("Mês", parts[3], 1, 12),
            dayOfWeek = field("Dia da semana", parts[4], 0, 6),
        )
    }

    private fun field(name: String, expression: String, min: Int, max: Int): CronField {
        for (unsupported in listOf("L", "W", "#", "?")) {
            if (expression.contains(unsupported)) {
                throw CronParseException("'$unsupported' no campo $name não é suportado.")
            }
        }

        val values = sortedSetOf<Int>()
        for (part in expression.split(",")) {
            val (rangePart, step) = if (part.contains("/")) {
                val halves = part.split("/")
                if (halves.size != 2) throw CronParseException("passo inválido em '$part' ($name).")
                val s = halves[1].toIntOrNull()
                    ?: throw CronParseException("passo '${halves[1]}' não é um número ($name).")
                if (s <= 0) throw CronParseException("passo precisa ser positivo ($name).")
                halves[0] to s
            } else {
                part to 1
            }

            val (from, to) = when {
                rangePart == "*" -> min to max
                rangePart.contains("-") -> {
                    val bounds = rangePart.split("-")
                    if (bounds.size != 2) throw CronParseException("intervalo inválido em '$part' ($name).")
                    val a = bounds[0].toIntOrNull()
                        ?: throw CronParseException("'${bounds[0]}' não é um número ($name).")
                    val b = bounds[1].toIntOrNull()
                        ?: throw CronParseException("'${bounds[1]}' não é um número ($name).")
                    a to b
                }
                else -> {
                    val v = rangePart.toIntOrNull()
                        ?: throw CronParseException("'$rangePart' não é um número ($name).")
                    v to v
                }
            }

            if (from < min || to > max || from > to) {
                throw CronParseException("$name aceita valores de $min a $max — recebido '$part'.")
            }
            var v = from
            while (v <= to) {
                values += v
                v += step
            }
        }
        return CronField(name, expression, values)
    }

    /** Descrição em linguagem natural, em pt-BR. */
    fun describe(cron: CronExpression): String {
        val time = describeTime(cron)
        val days = describeDays(cron)
        return "$time $days".replace("  ", " ").trim().replaceFirstChar { it.uppercase() }
    }

    private fun describeTime(cron: CronExpression): String {
        val minutes = cron.minute.values
        val hours = cron.hour.values

        return when {
            minutes.size == 60 && hours.size == 24 -> "a cada minuto"
            minutes.size == 60 -> "a cada minuto das horas ${hours.joinToString(", ")}"
            minutes.size > 1 && hours.size == 24 ->
                "nos minutos ${minutes.joinToString(", ")} de toda hora"
            hours.size == 24 -> "no minuto ${minutes.first()} de toda hora"
            minutes.size == 1 && hours.size == 1 ->
                "às " + pad(hours.first()) + ":" + pad(minutes.first())
            minutes.size == 1 ->
                "às " + hours.joinToString(", ") { pad(it) + ":" + pad(minutes.first()) }
            else -> "nos minutos ${minutes.joinToString(", ")} das horas ${hours.joinToString(", ")}"
        }
    }

    private fun describeDays(cron: CronExpression): String {
        val everyDom = cron.dayOfMonth.values.size == 31
        val everyDow = cron.dayOfWeek.values.size == 7
        val everyMonth = cron.month.values.size == 12

        val dowText = when {
            everyDow -> ""
            cron.dayOfWeek.values == setOf(1, 2, 3, 4, 5) -> "todos os dias úteis"
            cron.dayOfWeek.values == setOf(0, 6) -> "aos fins de semana"
            else -> "às " + cron.dayOfWeek.values.joinToString(", ") { WEEKDAY_NAMES[it] } + "s"
        }
        val domText = if (everyDom) "" else "nos dias ${cron.dayOfMonth.values.joinToString(", ")}"
        val monthText =
            if (everyMonth) "" else "em ${cron.month.values.joinToString(", ") { MONTH_NAMES[it - 1] }}"

        val pieces = listOf(dowText, domText, monthText).filter { it.isNotEmpty() }
        return when {
            pieces.isEmpty() -> "todos os dias"
            else -> pieces.joinToString(", ")
        }
    }

    /**
     * Próxima execução a partir de [from], por varredura de minutos.
     *
     * O limite de um ano evita laço infinito em expressões impossíveis (ex.: 30 de fevereiro).
     */
    @OptIn(ExperimentalTime::class)
    fun nextRun(cron: CronExpression, from: LocalDateTime, zone: TimeZone): LocalDateTime? {
        var instant = from.toInstant(zone)
            .plus(1, DateTimeUnit.MINUTE)
        val limit = from.toInstant(zone).plus(366, DateTimeUnit.DAY, zone)

        while (instant < limit) {
            val candidate = instant.toLocalDateTime(zone)
            if (matches(cron, candidate)) return candidate.withoutSeconds()
            instant = instant.plus(1, DateTimeUnit.MINUTE)
        }
        return null
    }

    fun matches(cron: CronExpression, time: LocalDateTime): Boolean {
        if (time.minute !in cron.minute.values) return false
        if (time.hour !in cron.hour.values) return false
        if (time.monthNumber !in cron.month.values) return false

        val domRestricted = cron.dayOfMonth.values.size != 31
        val dowRestricted = cron.dayOfWeek.values.size != 7
        // Regra do cron: com os dois campos restritos, basta um deles casar.
        val domMatch = time.dayOfMonth in cron.dayOfMonth.values
        val dowMatch = time.dayOfWeek.isoNumber % 7 in cron.dayOfWeek.values

        return when {
            domRestricted && dowRestricted -> domMatch || dowMatch
            domRestricted -> domMatch
            dowRestricted -> dowMatch
            else -> true
        }
    }

    fun describeField(field: CronField, kind: String): String = when {
        field.expression == "*" -> "* (qualquer)"
        kind == "dow" && field.values == setOf(1, 2, 3, 4, 5) ->
            "${field.expression} (segunda a sexta)"
        kind == "dow" ->
            "${field.expression} (${field.values.joinToString(", ") { WEEKDAY_NAMES[it] }})"
        kind == "month" ->
            "${field.expression} (${field.values.joinToString(", ") { MONTH_NAMES[it - 1] }})"
        else -> field.expression
    }

    private fun pad(v: Int) = v.toString().padStart(2, '0')

    private fun LocalDateTime.withoutSeconds() = LocalDateTime(year, monthNumber, dayOfMonth, hour, minute)
}

/** ISO 8601: segunda = 1 … domingo = 7. Não vem pronto no kotlinx-datetime 0.8. */
internal val kotlinx.datetime.DayOfWeek.isoNumber: Int get() = ordinal + 1

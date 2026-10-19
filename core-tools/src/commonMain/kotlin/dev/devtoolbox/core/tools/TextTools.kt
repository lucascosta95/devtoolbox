package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.MatchInfo
import dev.devtoolbox.core.Row
import dev.devtoolbox.core.Segment
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.CodePoints
import dev.devtoolbox.core.util.Lorem
import dev.devtoolbox.core.util.StringCase
import kotlin.math.abs

object StringCaseTool : Tool {
    override val id = "string-case"
    override val name = "String Case Converter"
    override val category = Category.Text
    override val icon = "text-aa"
    override val description =
        "Converta um texto entre lower, upper, camel, pascal, snake e kebab case."
    override val defaultInput = ToolInput.Text("hello devtoolbox world")

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        return ToolOutput.Success(
            ToolBody.Rows(
                listOf(
                    Row("lower case", StringCase.lower(text)),
                    Row("UPPER CASE", StringCase.upper(text)),
                    Row("Capitalized", StringCase.capitalized(text)),
                    Row("camelCase", StringCase.camel(text)),
                    Row("PascalCase", StringCase.pascal(text)),
                    Row("snake_case", StringCase.snake(text)),
                    Row("kebab-case", StringCase.kebab(text)),
                    Row("CONSTANT_CASE", StringCase.constant(text)),
                ),
            ),
        )
    }
}

object RegexTool : Tool {
    override val id = "regex"
    override val name = "Regex Tester"
    override val category = Category.Text
    override val icon = "magnifying-glass"
    override val description = "Teste uma expressão regular contra uma string de exemplo."
    override val defaultInput = ToolInput.Pattern(
        pattern = """(\d{3})-(\d{4})""",
        flags = "g",
        subject = "Suporte: 555-0132 ou 555-0148.",
    )

    override fun run(input: ToolInput): ToolOutput {
        val p = input as? ToolInput.Pattern ?: return ToolOutput.Failure("Entrada inválida.")
        if (p.pattern.isEmpty()) return ToolOutput.Failure("Informe um padrão.")

        val options = buildSet {
            if (p.flags.contains('i')) add(RegexOption.IGNORE_CASE)
            if (p.flags.contains('m')) add(RegexOption.MULTILINE)
            if (p.flags.contains('s')) add(RegexOption.DOT_MATCHES_ALL)
        }

        val regex = try {
            Regex(p.pattern, options)
        } catch (e: IllegalArgumentException) {
            return ToolOutput.Failure("Regex inválida: ${e.message?.lines()?.first() ?: "sintaxe"}")
        }

        val global = p.flags.contains('g')
        val found = regex.findAll(p.subject).let { if (global) it.toList() else it.take(1).toList() }

        val segments = mutableListOf<Segment>()
        var cursor = 0
        for (match in found) {
            if (match.range.first > cursor) {
                segments += Segment(p.subject.substring(cursor, match.range.first), matched = false)
            }
            segments += Segment(match.value, matched = true)
            cursor = match.range.last + 1
        }
        if (cursor < p.subject.length) {
            segments += Segment(p.subject.substring(cursor), matched = false)
        }

        return ToolOutput.Success(
            ToolBody.Regex(
                pattern = p.pattern,
                flags = p.flags,
                segments = segments,
                matches = found.mapIndexed { i, m -> MatchInfo(i, m.value) },
            ),
        )
    }
}

object LoremTool : Tool {
    override val id = "lorem"
    override val name = "Lorem Ipsum Generator"
    override val category = Category.Text
    override val icon = "paragraph"
    override val description = "Gere parágrafos de texto de preenchimento para protótipos."
    override val defaultInput = ToolInput.Seed(0)

    private const val PARAGRAPHS = 3

    override fun run(input: ToolInput): ToolOutput {
        val seed = (input as? ToolInput.Seed)?.nonce ?: 0
        val paragraphs = Lorem.paragraphs(PARAGRAPHS, seed)
        return ToolOutput.Success(
            ToolBody.Rows(
                rows = paragraphs.mapIndexed { i, p -> Row("Parágrafo ${i + 1}", p) },
                regenerable = true,
            ),
        )
    }
}

object SubstringTool : Tool {
    override val id = "substring"
    override val name = "Substring Tester"
    override val category = Category.Text
    override val icon = "selection"
    override val description =
        "Extraia um trecho de uma string pela posição inicial e pelo comprimento, linha a linha se quiser."
    override val defaultInput = ToolInput.Slice(
        text = "https://devtoolbox.dev/tools?id=substring",
        start = "9",
        length = "14",
    )

    private val INTEGER = Regex("[+-]?\\d+")

    // Mesma semântica do onlinestringtools.com/extract-substring: posições começam em 1,
    // posição negativa conta do fim (-1 é o último caractere) e o sinal do comprimento é a
    // direção do corte (negativo recorta para a esquerda terminando na posição inicial).
    // Comprimento vazio vai até o fim; só "-" vai até o começo.
    override fun run(input: ToolInput): ToolOutput {
        val slice = input as? ToolInput.Slice ?: return ToolOutput.Failure("Entrada inválida.")
        val lines = if (slice.multiLine) slice.text.split("\n") else listOf(slice.text)
        val points = CodePoints.split(slice.text)
        val untouched = ToolBody.Substring(
            text = slice.text,
            length = points.size,
            segments = if (slice.text.isEmpty()) emptyList() else listOf(Segment(slice.text, matched = false)),
            result = "",
        )

        val startText = slice.start.trim()
        val lengthText = slice.length.trim()
        if (!INTEGER.matches(startText)) {
            return ToolOutput.Failure("A posição inicial não é um número inteiro válido.", untouched)
        }
        if (!INTEGER.matches(lengthText) && lengthText != "" && lengthText != "-") {
            return ToolOutput.Failure("O comprimento não é um número inteiro válido.", untouched)
        }
        val start = startText.toIntOrNull()
            ?: return ToolOutput.Failure("A posição inicial é grande demais.", untouched)

        val segments = mutableListOf<Segment>()
        val pieces = mutableListOf<String>()
        val shortLines = mutableListOf<Int>()
        for ((index, line) in lines.withIndex()) {
            if (index > 0) segments += Segment("\n", matched = false)
            val chars = CodePoints.split(line)
            val cut = cut(chars.size, start, lengthText)
                ?: return ToolOutput.Failure("O comprimento é grande demais.", untouched)
            if (cut.incomplete) shortLines += index + 1

            val before = chars.subList(0, cut.from).joinToString("")
            val selected = chars.subList(cut.from, cut.to)
            val after = chars.subList(cut.to, chars.size).joinToString("")
            if (before.isNotEmpty()) segments += Segment(before, matched = false)
            if (selected.isNotEmpty()) segments += Segment(selected.joinToString(""), matched = true)
            if (after.isNotEmpty()) segments += Segment(after, matched = false)

            pieces += (if (slice.reverse) selected.asReversed() else selected).joinToString("")
        }

        val warning = when {
            shortLines.isEmpty() -> null
            !slice.multiLine -> "Não havia caracteres suficientes para o comprimento pedido."
            else -> "Não havia caracteres suficientes na${if (shortLines.size > 1) "s linhas" else " linha"} " +
                shortLines.joinToString(", ") + "."
        }

        return ToolOutput.Success(
            ToolBody.Substring(
                text = slice.text,
                length = points.size,
                segments = segments,
                result = pieces.joinToString("\n"),
                warning = warning,
            ),
        )
    }

    private class Cut(val from: Int, val to: Int, val incomplete: Boolean)

    private fun cut(size: Int, start: Int, lengthText: String): Cut? {
        val length: Long = when (lengthText) {
            "" -> if (start > 0) size - start + 1L else -start.toLong()
            "-" -> if (start > 0) -start.toLong() else -size - start - 1L
            else -> lengthText.toLongOrNull() ?: return null
        }
        val (from, to) = when {
            length > 0 -> {
                val from = if (start > 0) start - 1L else size.toLong() + start
                from to from + length
            }
            length < 0 -> {
                val to = if (start > 0) start.toLong() else size.toLong() + start + 1
                to + length to to
            }
            else -> 0L to 0L
        }
        val clampedFrom = from.coerceIn(0, size.toLong()).toInt()
        val clampedTo = to.coerceIn(clampedFrom.toLong(), size.toLong()).toInt()
        // Só avisa quando o comprimento foi digitado; vazio e "-" já significam "até a borda".
        val explicit = lengthText != "" && lengthText != "-"
        val incomplete = explicit && clampedTo - clampedFrom < abs(length)
        return Cut(clampedFrom, clampedTo, incomplete)
    }
}

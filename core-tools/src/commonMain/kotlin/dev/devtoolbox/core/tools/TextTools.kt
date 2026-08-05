package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.MatchInfo
import dev.devtoolbox.core.Row
import dev.devtoolbox.core.Segment
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.Lorem
import dev.devtoolbox.core.util.StringCase

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

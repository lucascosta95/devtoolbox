package dev.devtoolbox.core.util

/** Ajustes do [formatNrql]. A UI ainda não expõe controles — os padrões são os do protótipo. */
data class NrqlFormatOptions(
    val indentSize: Int = 2,
    val uppercaseClauses: Boolean = true,
)

sealed interface NrqlFormatResult {
    data class Success(val nrql: String) : NrqlFormatResult

    data class Failure(val message: String) : NrqlFormatResult
}

/**
 * Indenta NRQL (New Relic) e dialetos de observabilidade parecidos.
 *
 * Não é o [formatSql] com outro vocabulário: NRQL não tem JOIN nem subconsulta correlacionada,
 * mas tem janela de tempo em linguagem natural (`SINCE 3 hours ago`) e funções em camelCase que
 * não podem virar maiúsculas. Só o léxico é comum às duas ferramentas.
 *
 * Como no SQL: sem validação de sintaxe, entrada irreconhecível volta normalizada, e formatar a
 * saída de novo devolve o mesmo texto.
 */
fun formatNrql(input: String, options: NrqlFormatOptions = NrqlFormatOptions()): NrqlFormatResult {
    if (input.isBlank()) return NrqlFormatResult.Failure("Entrada vazia.")
    val tokens = tokenizeQuery(input)
    if (tokens.isEmpty()) return NrqlFormatResult.Failure("Entrada vazia.")
    return NrqlFormatResult.Success(formatQuery(tokens, options))
}

// ---------------------------------------------------------------------------------------------
// Vocabulário
// ---------------------------------------------------------------------------------------------

/** Cláusulas: cada uma abre linha na coluna 0 do nível corrente. */
private val CLAUSES = setOf(
    "SELECT", "FROM", "WHERE", "FACET", "SINCE", "UNTIL", "TIMESERIES", "COMPARE WITH",
    "LIMIT", "OFFSET", "ORDER BY", "WITH", "SHOW EVENT TYPES",
)

/** Operadores e literais que ganham maiúscula — nunca atributos nem nomes de evento. */
private val OPERATORS = setOf(
    "AND", "OR", "NOT", "IN", "LIKE", "IS", "NULL", "AS", "TRUE", "FALSE",
    "ASC", "DESC", "MAX", "AUTO", "RAW",
)

private val PHRASES = listOf(
    listOf("SHOW", "EVENT", "TYPES"),
    listOf("COMPARE", "WITH"),
    listOf("ORDER", "BY"),
).sortedByDescending { it.size }

/** Só `SELECT` e `FACET` quebram a lista de itens uma por linha. */
private val LIST_CLAUSES = setOf("SELECT", "FACET")

/** Cláusulas de janela de tempo, onde as palavras de duração ficam em minúsculas. */
private val TIME_CLAUSES = setOf("SINCE", "UNTIL", "TIMESERIES", "COMPARE WITH")

private val DURATION_WORDS = setOf(
    "ago", "second", "seconds", "minute", "minutes", "hour", "hours",
    "day", "days", "week", "weeks", "month", "months", "year", "years",
)

/**
 * Palavras que continuam palavras-chave mesmo grudadas em um `(`.
 *
 * O resto é decidido pela posição: `max(duration)` é função e sai como veio, `LIMIT MAX` é
 * literal e sobe para maiúscula — a mesma palavra, papéis diferentes.
 */
private val NEVER_FUNCTIONS = CLAUSES + setOf("AND", "OR", "NOT", "IN", "LIKE", "IS", "AS")

// ---------------------------------------------------------------------------------------------
// Formatação
// ---------------------------------------------------------------------------------------------

/** Estado salvo ao entrar em um parêntese e restaurado ao sair dele. */
private class NrqlFrame(
    val nested: Boolean,
    val base: Int,
    val listIndent: Int?,
    val condIndent: Int,
    val exprDepth: Int,
    val timeClause: Boolean,
)

private fun formatQuery(tokens: List<QueryToken>, options: NrqlFormatOptions): String {
    val writer = LineWriter(options.indentSize)
    val frames = ArrayDeque<NrqlFrame>()

    var base = 0
    var listIndent: Int? = null
    var condIndent = 1
    var exprDepth = 0
    var timeClause = false
    var prev: QueryToken? = null

    /** [cur] decide o espaçamento; [last] é o token que fica como anterior (fim da locução). */
    fun emit(text: String, cur: QueryToken, last: QueryToken = cur) {
        writer.append(text, spaceBetween(prev, cur, writer.lineIsEmpty, ::roleOf))
        prev = last
    }

    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        var consumed = 1

        when {
            token.kind == TokenKind.LineComment -> {
                emit(token.text, token)
                writer.flush()
            }

            token.kind == TokenKind.Punct && token.text == "(" -> {
                // `filter(…)` e `FROM (SELECT …)` ganham nível próprio; o resto fica inline.
                val nested = opensNestedQuery(tokens, i)
                emit("(", token)
                frames.addLast(NrqlFrame(nested, base, listIndent, condIndent, exprDepth, timeClause))
                listIndent = null
                if (nested) {
                    base += 1
                    condIndent = base + 1
                    exprDepth = 0
                    timeClause = false
                    writer.startLine(base)
                } else {
                    exprDepth++
                }
            }

            token.kind == TokenKind.Punct && token.text == ")" -> {
                val frame = frames.removeLastOrNull()
                if (frame != null && frame.nested) writer.startLine(frame.base)
                emit(")", token)
                if (frame != null) {
                    base = frame.base
                    listIndent = frame.listIndent
                    condIndent = frame.condIndent
                    exprDepth = frame.exprDepth
                    timeClause = frame.timeClause
                }
            }

            token.kind == TokenKind.Punct && token.text == "," -> {
                emit(",", token)
                listIndent?.let { writer.startLine(it) }
            }

            token.kind == TokenKind.Word -> {
                val phrase = matchPhrase(tokens, i)
                val canonical = phrase?.first ?: token.text.uppercase()
                consumed = phrase?.second ?: 1
                val last = tokens[i + consumed - 1]
                val text = render(tokens, i, consumed, canonical, timeClause, options)

                when {
                    canonical in CLAUSES && exprDepth == 0 -> {
                        writer.startLine(base)
                        listIndent = null
                        timeClause = canonical in TIME_CLAUSES
                        emit(text, token, last)
                        if (canonical == "WHERE") condIndent = base + 1
                        if (canonical in LIST_CLAUSES &&
                            countTopLevelCommas(tokens, i + consumed) > 0
                        ) {
                            listIndent = base + 1
                            writer.startLine(base + 1)
                        }
                    }

                    // Parêntese de expressão simples fica inline, com AND/OR e tudo.
                    (canonical == "AND" || canonical == "OR") && exprDepth == 0 -> {
                        writer.startLine(condIndent)
                        emit(text, token, last)
                    }

                    else -> emit(text, token, last)
                }
            }

            else -> emit(token.text, token)
        }
        i += consumed
    }
    return writer.build()
}

/**
 * Decide a grafia de uma palavra.
 *
 * A regra que sustenta `count()` e `uniqueCount()` é posicional, não um catálogo: palavra
 * seguida de `(` é chamada de função e sai como veio — inclusive as funções que este formatador
 * não conhece.
 */
private fun render(
    tokens: List<QueryToken>,
    index: Int,
    length: Int,
    canonical: String,
    inTimeClause: Boolean,
    options: NrqlFormatOptions,
): String {
    val token = tokens[index]
    val original = (0 until length).joinToString(" ") { tokens[index + it].text }
    if (!options.uppercaseClauses) return original
    if (length > 1 || canonical in NEVER_FUNCTIONS) return canonical
    if (isFunctionCall(tokens, index)) return token.text
    if (inTimeClause && token.text.lowercase() in DURATION_WORDS) return token.text.lowercase()
    if (canonical in OPERATORS) return canonical
    return token.text
}

private fun isFunctionCall(tokens: List<QueryToken>, index: Int): Boolean {
    val next = tokens.getOrNull(index + 1) ?: return false
    return next.kind == TokenKind.Punct && next.text == "("
}

/**
 * Só é consultado para o token que antecede um `(`: cláusula respira (`FROM (SELECT …)`),
 * qualquer outra palavra ali é nome de função e cola no parêntese.
 */
private fun roleOf(token: QueryToken): WordRole {
    val upper = token.text.uppercase()
    val keyword = upper in NEVER_FUNCTIONS || PHRASES.any { it.last() == upper }
    return if (keyword) WordRole.Keyword else WordRole.Function
}

private fun matchPhrase(tokens: List<QueryToken>, start: Int): Pair<String, Int>? {
    for (phrase in PHRASES) {
        if (start + phrase.size > tokens.size) continue
        val matches = phrase.indices.all { offset ->
            val candidate = tokens[start + offset]
            candidate.kind == TokenKind.Word && candidate.text.uppercase() == phrase[offset]
        }
        if (matches) return phrase.joinToString(" ") to phrase.size
    }
    return null
}

/** Um parêntese que carrega cláusula dentro — `FROM (SELECT …)`, `filter(…, WHERE …)`. */
private fun opensNestedQuery(tokens: List<QueryToken>, openIndex: Int): Boolean {
    var depth = 0
    var i = openIndex + 1
    while (i < tokens.size) {
        val token = tokens[i]
        when {
            token.kind == TokenKind.Punct && token.text == "(" -> depth++
            token.kind == TokenKind.Punct && token.text == ")" -> {
                if (depth == 0) return false
                depth--
            }
            token.kind == TokenKind.Word && depth == 0 -> {
                val phrase = matchPhrase(tokens, i)
                val canonical = phrase?.first ?: token.text.uppercase()
                if (canonical in CLAUSES) return true
            }
        }
        i++
    }
    return false
}

/** Vírgulas da lista corrente — só as do nível do parêntese e só até a próxima cláusula. */
private fun countTopLevelCommas(tokens: List<QueryToken>, start: Int): Int {
    var commas = 0
    var depth = 0
    var i = start
    while (i < tokens.size) {
        val token = tokens[i]
        when {
            token.kind == TokenKind.Punct && token.text == "(" -> depth++
            token.kind == TokenKind.Punct && token.text == ")" -> {
                if (depth == 0) return commas
                depth--
            }
            token.kind == TokenKind.Punct && token.text == "," && depth == 0 -> commas++
            token.kind == TokenKind.Word && depth == 0 -> {
                val phrase = matchPhrase(tokens, i)
                val canonical = phrase?.first ?: token.text.uppercase()
                if (canonical in CLAUSES) return commas
                if (phrase != null) i += phrase.second - 1
            }
        }
        i++
    }
    return commas
}

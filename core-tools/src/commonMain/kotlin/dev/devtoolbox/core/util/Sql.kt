package dev.devtoolbox.core.util

data class SqlFormatOptions(
    val indentSize: Int = 2,
    val uppercaseKeywords: Boolean = true,
)

sealed interface SqlFormatResult {
    data class Success(val sql: String) : SqlFormatResult

    data class Failure(val message: String) : SqlFormatResult
}

fun formatSql(input: String, options: SqlFormatOptions = SqlFormatOptions()): SqlFormatResult {
    if (input.isBlank()) return SqlFormatResult.Failure("Entrada vazia.")
    val statements = splitStatements(tokenizeQuery(input))
        .map { formatStatement(it, options) }
        .filter { it.isNotEmpty() }
    if (statements.isEmpty()) return SqlFormatResult.Failure("Entrada vazia.")
    return SqlFormatResult.Success(statements.joinToString("\n\n"))
}

private val KEYWORDS = setOf(
    "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "IS", "NULL", "TRUE", "FALSE", "AS",
    "DISTINCT", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS", "ON", "USING",
    "GROUP", "HAVING", "ORDER", "ASC", "DESC", "LIMIT", "OFFSET", "UNION",
    "INSERT", "VALUES", "UPDATE", "SET", "DELETE", "WITH", "CASE", "WHEN", "THEN",
    "ELSE", "END", "BETWEEN", "LIKE", "EXISTS", "COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE",
)

private val FUNCTIONS = setOf("COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE")

private val PHRASES = listOf(
    listOf("LEFT", "OUTER", "JOIN"),
    listOf("RIGHT", "OUTER", "JOIN"),
    listOf("FULL", "OUTER", "JOIN"),
    listOf("GROUP", "BY"),
    listOf("ORDER", "BY"),
    listOf("UNION", "ALL"),
    listOf("INSERT", "INTO"),
    listOf("DELETE", "FROM"),
    listOf("INNER", "JOIN"),
    listOf("LEFT", "JOIN"),
    listOf("RIGHT", "JOIN"),
    listOf("FULL", "JOIN"),
    listOf("CROSS", "JOIN"),
).sortedByDescending { it.size }

private val CLAUSES = setOf(
    "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY", "LIMIT", "OFFSET",
    "UNION", "UNION ALL", "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM", "DELETE",
    "WITH",
)

private val JOINS = setOf(
    "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "CROSS JOIN",
    "LEFT OUTER JOIN", "RIGHT OUTER JOIN", "FULL OUTER JOIN",
)

private val LIST_CLAUSES = setOf("SELECT", "GROUP BY")

private class SqlFrame(
    val subquery: Boolean,
    val base: Int,
    val listIndent: Int?,
    val condIndent: Int,
    val exprDepth: Int,
    val caseDepth: Int,
)

private fun splitStatements(tokens: List<QueryToken>): List<List<QueryToken>> {
    val statements = mutableListOf<List<QueryToken>>()
    var current = mutableListOf<QueryToken>()
    var depth = 0
    for (token in tokens) {
        when (token.text) {
            "(" -> depth++
            ")" -> if (depth > 0) depth--
        }
        current += token
        if (token.text == ";" && token.kind == TokenKind.Punct && depth == 0) {
            statements += current
            current = mutableListOf()
        }
    }
    if (current.isNotEmpty()) statements += current
    return statements
}

private fun formatStatement(tokens: List<QueryToken>, options: SqlFormatOptions): String {
    val writer = LineWriter(options.indentSize)
    val frames = ArrayDeque<SqlFrame>()

    val cases = ArrayDeque<Int>()

    var base = 0
    var listIndent: Int? = null
    var condIndent = 1
    var exprDepth = 0
    var betweenPending = false
    var inWith = false
    var cteBoundary = false
    var prev: QueryToken? = null

    fun emit(text: String, cur: QueryToken, last: QueryToken = cur) {
        writer.append(text, spaceBetween(prev, cur, writer.lineIsEmpty, ::roleOf))
        prev = last
    }

    fun word(token: QueryToken, phrase: String? = null): String {
        val canonical = phrase ?: token.text.uppercase()
        val isKeyword = phrase != null || canonical in KEYWORDS
        return if (isKeyword && options.uppercaseKeywords) canonical else token.text
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
                val subquery = opensSubquery(tokens, i)
                emit("(", token)
                frames.addLast(SqlFrame(subquery, base, listIndent, condIndent, exprDepth, cases.size))
                listIndent = null
                if (subquery) {
                    base += 1
                    condIndent = base + 1
                    exprDepth = 0
                } else {
                    exprDepth++
                }
            }

            token.kind == TokenKind.Punct && token.text == ")" -> {
                val frame = frames.removeLastOrNull()
                if (frame != null && frame.subquery) writer.startLine(frame.base)
                emit(")", token)
                if (frame != null) {
                    base = frame.base
                    listIndent = frame.listIndent
                    condIndent = frame.condIndent
                    exprDepth = frame.exprDepth
                    while (cases.size > frame.caseDepth) cases.removeLast()
                    if (frame.subquery && inWith && frames.isEmpty()) cteBoundary = true
                }
            }

            token.kind == TokenKind.Punct && token.text == "," -> {
                emit(",", token)
                if (cteBoundary) {
                    writer.blankLine()
                    cteBoundary = false
                } else {
                    listIndent?.let { writer.startLine(it) }
                }
            }

            token.kind == TokenKind.Word -> {
                val phrase = matchPhrase(tokens, i)
                val canonical = phrase?.first ?: token.text.uppercase()
                consumed = phrase?.second ?: 1
                val text = word(token, phrase?.first)
                val last = tokens[i + consumed - 1]

                when {
                    canonical in CLAUSES && exprDepth == 0 -> {
                        if (cteBoundary) {
                            inWith = false
                            cteBoundary = false
                        }
                        writer.startLine(base)
                        listIndent = null
                        betweenPending = false
                        emit(text, token, last)
                        when (canonical) {
                            "WITH" -> inWith = true
                            "WHERE", "HAVING" -> condIndent = base + 1
                        }
                        if (canonical in LIST_CLAUSES) {
                            var next = i + consumed
                            if (canonical == "SELECT") {
                                val modifier = tokens.getOrNull(next)
                                if (modifier != null && modifier.kind == TokenKind.Word &&
                                    modifier.text.uppercase() in setOf("DISTINCT", "ALL")
                                ) {
                                    emit(word(modifier, modifier.text.uppercase()), modifier)
                                    next++
                                    consumed++
                                }
                            }
                            if (countTopLevelCommas(tokens, next) > 0) {
                                listIndent = base + 1
                                writer.startLine(base + 1)
                            }
                        }
                    }

                    canonical in JOINS && exprDepth == 0 -> {
                        writer.startLine(base + 1)
                        listIndent = null
                        emit(text, token, last)
                    }

                    (canonical == "ON" || canonical == "USING") && exprDepth == 0 -> {
                        writer.startLine(base + 2)
                        emit(text, token, last)
                        condIndent = base + 3
                    }

                    canonical == "AND" || canonical == "OR" -> {
                        val inlineAnd = betweenPending && canonical == "AND"
                        if (inlineAnd) betweenPending = false
                        if (!inlineAnd && exprDepth == 0) writer.startLine(condIndent)
                        emit(text, token, last)
                    }

                    canonical == "CASE" -> {
                        emit(text, token, last)
                        cases.addLast(writer.indent)
                    }

                    canonical == "WHEN" || canonical == "THEN" || canonical == "ELSE" -> {
                        cases.lastOrNull()?.let { writer.startLine(it + 1) }
                        emit(text, token, last)
                    }

                    canonical == "END" -> {
                        cases.removeLastOrNull()?.let { writer.startLine(it) }
                        emit(text, token, last)
                    }

                    canonical == "BETWEEN" -> {
                        emit(text, token, last)
                        betweenPending = true
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

private fun roleOf(token: QueryToken): WordRole {
    val upper = token.text.uppercase()
    return when {
        upper in FUNCTIONS -> WordRole.Function
        upper in KEYWORDS -> WordRole.Keyword
        else -> WordRole.Identifier
    }
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

private fun opensSubquery(tokens: List<QueryToken>, openIndex: Int): Boolean {
    var i = openIndex + 1
    while (i < tokens.size) {
        val token = tokens[i]
        if (token.kind == TokenKind.LineComment || token.kind == TokenKind.BlockComment) {
            i++
            continue
        }
        return token.kind == TokenKind.Word && token.text.uppercase() in setOf("SELECT", "WITH")
    }
    return false
}

private fun countTopLevelCommas(tokens: List<QueryToken>, start: Int): Int {
    var commas = 0
    var depth = 0
    var i = start
    while (i < tokens.size) {
        val token = tokens[i]
        when {
            token.text == "(" && token.kind == TokenKind.Punct -> depth++
            token.text == ")" && token.kind == TokenKind.Punct -> {
                if (depth == 0) return commas
                depth--
            }
            token.text == ";" && token.kind == TokenKind.Punct && depth == 0 -> return commas
            token.text == "," && token.kind == TokenKind.Punct && depth == 0 -> commas++
            token.kind == TokenKind.Word && depth == 0 -> {
                val phrase = matchPhrase(tokens, i)
                val canonical = phrase?.first ?: token.text.uppercase()
                if (canonical in CLAUSES || canonical in JOINS) return commas
                if (phrase != null) i += phrase.second - 1
            }
        }
        i++
    }
    return commas
}

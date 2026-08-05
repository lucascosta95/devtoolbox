package dev.devtoolbox.core.util

internal enum class TokenKind { Word, Number, Text, LineComment, BlockComment, Punct }

internal data class QueryToken(
    val text: String,
    val kind: TokenKind,
    val glued: Boolean,
)

private val TWO_CHAR_OPERATORS = setOf("!=", "<>", ">=", "<=", "||", "::", "->")

internal fun tokenizeQuery(source: String): List<QueryToken> {
    val tokens = mutableListOf<QueryToken>()
    var gap = true
    var i = 0

    fun add(text: String, kind: TokenKind) {
        tokens += QueryToken(text, kind, glued = !gap && tokens.isNotEmpty())
        gap = false
    }

    while (i < source.length) {
        val c = source[i]
        when {
            c.isWhitespace() -> {
                gap = true
                i++
            }

            c == '-' && source.startsWith("--", i) -> {
                val end = source.indexOf('\n', i).takeIf { it >= 0 } ?: source.length
                add(source.substring(i, end).trimEnd(), TokenKind.LineComment)
                i = end
            }

            c == '/' && source.startsWith("/*", i) -> {
                val close = source.indexOf("*/", i + 2)
                val end = if (close >= 0) close + 2 else source.length
                add(source.substring(i, end), TokenKind.BlockComment)
                i = end
            }

            c == '\'' || c == '"' || c == '`' -> {
                val sb = StringBuilder().append(c)
                var j = i + 1
                var closed = false
                while (j < source.length && !closed) {
                    val d = source[j]
                    when {
                        d == '\\' && j + 1 < source.length -> {
                            sb.append(d).append(source[j + 1]); j += 2
                        }
                        d == c && j + 1 < source.length && source[j + 1] == c -> {
                            sb.append(c).append(c); j += 2
                        }
                        d == c -> {
                            sb.append(c); j++; closed = true
                        }
                        else -> {
                            sb.append(d); j++
                        }
                    }
                }
                add(sb.toString(), TokenKind.Text)
                i = j
            }

            c.isDigit() -> {
                var j = i
                while (j < source.length && (source[j].isDigit() || source[j] == '.')) j++
                if (j < source.length && (source[j] == 'e' || source[j] == 'E')) {
                    var k = j + 1
                    if (k < source.length && (source[k] == '+' || source[k] == '-')) k++
                    if (k < source.length && source[k].isDigit()) {
                        while (k < source.length && source[k].isDigit()) k++
                        j = k
                    }
                }
                add(source.substring(i, j), TokenKind.Number)
                i = j
            }

            isWordStart(c) -> {
                var j = i
                while (j < source.length && isWordPart(source[j])) j++
                add(source.substring(i, j), TokenKind.Word)
                i = j
            }

            else -> {
                val two = if (i + 1 < source.length) source.substring(i, i + 2) else ""
                if (two in TWO_CHAR_OPERATORS) {
                    add(two, TokenKind.Punct)
                    i += 2
                } else {
                    add(c.toString(), TokenKind.Punct)
                    i++
                }
            }
        }
    }
    return tokens
}

private fun isWordStart(c: Char) = c.isLetter() || c == '_' || c == '@' || c == '#' || c == '$'

private fun isWordPart(c: Char) = c.isLetterOrDigit() || c == '_' || c == '@' || c == '#' ||
    c == '$' || c == '.'

internal class LineWriter(private val indentSize: Int) {
    private val lines = mutableListOf<String>()
    private val current = StringBuilder()

    var indent: Int = 0
        private set

    val lineIsEmpty: Boolean get() = current.isEmpty()

    fun startLine(units: Int) {
        flush()
        indent = units
    }

    fun append(text: String, space: Boolean) {
        if (current.isNotEmpty() && space) current.append(' ')
        current.append(text)
    }

    fun flush() {
        if (current.isNotEmpty()) {
            lines += " ".repeat(indent * indentSize) + current
            current.clear()
        }
    }

    fun blankLine() {
        flush()
        if (lines.isNotEmpty() && lines.last().isNotEmpty()) lines += ""
    }

    fun build(): String {
        flush()
        return lines.joinToString("\n").trimEnd()
    }
}

internal enum class WordRole { Function, Keyword, Identifier }

internal fun spaceBetween(
    prev: QueryToken?,
    cur: QueryToken,
    lineIsEmpty: Boolean,
    roleOf: (QueryToken) -> WordRole,
): Boolean {
    if (lineIsEmpty || prev == null) return false
    if (cur.text == "," || cur.text == ";" || cur.text == ")") return false
    if (prev.text == "(") return false
    if (prev.text.endsWith(".")) return false
    if (prev.text == "::" || cur.text == "::") return false
    if (cur.text == "(") return spaceBeforeOpenParen(prev, cur, roleOf)
    return true
}

private fun spaceBeforeOpenParen(
    prev: QueryToken,
    cur: QueryToken,
    roleOf: (QueryToken) -> WordRole,
): Boolean = when {
    prev.kind != TokenKind.Word -> true
    else -> when (roleOf(prev)) {
        WordRole.Function -> false
        WordRole.Keyword -> true
        WordRole.Identifier -> !cur.glued
    }
}

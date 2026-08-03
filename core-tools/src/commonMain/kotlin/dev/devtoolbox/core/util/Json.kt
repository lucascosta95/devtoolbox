package dev.devtoolbox.core.util

/**
 * Parser/serializador JSON mínimo, próprio, em `commonMain`.
 *
 * Motivo de não usar kotlinx-serialization aqui: precisamos preservar a **ordem das chaves**,
 * manter os números exatamente como escritos e produzir erros com **posição** ("linha 3, coluna 12").
 * Também é reaproveitado pelo JWT Decoder e pela normalização do diff de JSON.
 */
sealed interface JsonValue {
    data object Null : JsonValue
    data class Bool(val value: Boolean) : JsonValue

    /** O número é guardado como texto para não perder formato (1.0, 1e5, precisão longa). */
    data class Num(val literal: String) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Obj(val entries: List<Pair<String, JsonValue>>) : JsonValue
}

class JsonParseException(message: String) : Exception(message)

object Json {

    fun parse(text: String): JsonValue {
        val p = Parser(text)
        p.skipWhitespace()
        val value = p.parseValue()
        p.skipWhitespace()
        if (!p.atEnd()) p.fail("conteúdo extra após o fim do JSON")
        return value
    }

    /** Reemite com indentação de [indent] espaços, preservando a ordem das chaves. */
    fun pretty(value: JsonValue, indent: Int = 2): String =
        StringBuilder().also { write(it, value, indent, 0) }.toString()

    /** Reemite sem espaços supérfluos. */
    fun compact(value: JsonValue): String =
        StringBuilder().also { write(it, value, 0, 0) }.toString()

    fun format(text: String, indent: Int = 2): String = pretty(parse(text), indent)

    private fun write(sb: StringBuilder, value: JsonValue, indent: Int, depth: Int) {
        val nl = if (indent > 0) "\n" else ""
        val pad = if (indent > 0) " ".repeat(indent * (depth + 1)) else ""
        val padEnd = if (indent > 0) " ".repeat(indent * depth) else ""
        val colon = if (indent > 0) ": " else ":"
        when (value) {
            is JsonValue.Null -> sb.append("null")
            is JsonValue.Bool -> sb.append(if (value.value) "true" else "false")
            is JsonValue.Num -> sb.append(value.literal)
            is JsonValue.Str -> writeString(sb, value.value)
            is JsonValue.Arr -> {
                if (value.items.isEmpty()) { sb.append("[]"); return }
                sb.append('[').append(nl)
                value.items.forEachIndexed { i, item ->
                    sb.append(pad)
                    write(sb, item, indent, depth + 1)
                    if (i != value.items.lastIndex) sb.append(',')
                    sb.append(nl)
                }
                sb.append(padEnd).append(']')
            }
            is JsonValue.Obj -> {
                if (value.entries.isEmpty()) { sb.append("{}"); return }
                sb.append('{').append(nl)
                value.entries.forEachIndexed { i, (key, v) ->
                    sb.append(pad)
                    writeString(sb, key)
                    sb.append(colon)
                    write(sb, v, indent, depth + 1)
                    if (i != value.entries.lastIndex) sb.append(',')
                    sb.append(nl)
                }
                sb.append(padEnd).append('}')
            }
        }
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else ->
                    if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                    else sb.append(c)
            }
        }
        sb.append('"')
    }

    private class Parser(val text: String) {
        var pos = 0

        fun atEnd() = pos >= text.length

        fun fail(reason: String): Nothing {
            var line = 1
            var col = 1
            for (i in 0 until minOf(pos, text.length)) {
                if (text[i] == '\n') { line++; col = 1 } else col++
            }
            throw JsonParseException("JSON inválido na linha $line, coluna $col: $reason")
        }

        fun skipWhitespace() {
            while (pos < text.length && text[pos].isJsonWhitespace()) pos++
        }

        fun parseValue(): JsonValue {
            if (atEnd()) fail("fim inesperado do documento")
            return when (val c = text[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonValue.Str(parseString())
                't' -> literal("true", JsonValue.Bool(true))
                'f' -> literal("false", JsonValue.Bool(false))
                'n' -> literal("null", JsonValue.Null)
                else ->
                    if (c == '-' || c in '0'..'9') parseNumber()
                    else fail("caractere inesperado '$c'")
            }
        }

        fun literal(word: String, value: JsonValue): JsonValue {
            if (!text.startsWith(word, pos)) fail("token inesperado")
            pos += word.length
            return value
        }

        fun parseObject(): JsonValue {
            pos++ // '{'
            val entries = mutableListOf<Pair<String, JsonValue>>()
            skipWhitespace()
            if (!atEnd() && text[pos] == '}') { pos++; return JsonValue.Obj(entries) }
            while (true) {
                skipWhitespace()
                if (atEnd() || text[pos] != '"') fail("esperava uma chave entre aspas")
                val key = parseString()
                skipWhitespace()
                if (atEnd() || text[pos] != ':') fail("esperava ':' após a chave \"$key\"")
                pos++
                skipWhitespace()
                entries += key to parseValue()
                skipWhitespace()
                if (atEnd()) fail("objeto não fechado")
                when (text[pos]) {
                    ',' -> pos++
                    '}' -> { pos++; return JsonValue.Obj(entries) }
                    else -> fail("esperava ',' ou '}'")
                }
            }
        }

        fun parseArray(): JsonValue {
            pos++ // '['
            val items = mutableListOf<JsonValue>()
            skipWhitespace()
            if (!atEnd() && text[pos] == ']') { pos++; return JsonValue.Arr(items) }
            while (true) {
                skipWhitespace()
                items += parseValue()
                skipWhitespace()
                if (atEnd()) fail("array não fechado")
                when (text[pos]) {
                    ',' -> pos++
                    ']' -> { pos++; return JsonValue.Arr(items) }
                    else -> fail("esperava ',' ou ']'")
                }
            }
        }

        fun parseString(): String {
            pos++ // '"'
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) fail("string não fechada")
                when (val c = text[pos]) {
                    '"' -> { pos++; return sb.toString() }
                    '\\' -> {
                        pos++
                        if (atEnd()) fail("escape incompleto")
                        when (val e = text[pos]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 >= text.length) fail("escape \\u incompleto")
                                val hex = text.substring(pos + 1, pos + 5)
                                val code = hex.toIntOrNull(16) ?: fail("escape \\u$hex inválido")
                                sb.append(code.toChar())
                                pos += 4
                            }
                            else -> fail("escape \\$e desconhecido")
                        }
                        pos++
                    }
                    else -> { sb.append(c); pos++ }
                }
            }
        }

        fun parseNumber(): JsonValue {
            val start = pos
            if (!atEnd() && text[pos] == '-') pos++
            while (!atEnd() && text[pos] in '0'..'9') pos++
            if (!atEnd() && text[pos] == '.') {
                pos++
                while (!atEnd() && text[pos] in '0'..'9') pos++
            }
            if (!atEnd() && (text[pos] == 'e' || text[pos] == 'E')) {
                pos++
                if (!atEnd() && (text[pos] == '+' || text[pos] == '-')) pos++
                while (!atEnd() && text[pos] in '0'..'9') pos++
            }
            val literal = text.substring(start, pos)
            if (literal.isEmpty() || literal == "-") fail("número inválido")
            return JsonValue.Num(literal)
        }
    }
}

private fun Char.isJsonWhitespace() = this == ' ' || this == '\t' || this == '\n' || this == '\r'

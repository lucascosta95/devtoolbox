package dev.devtoolbox.core.util

sealed interface YamlValue {
    data class Scalar(val text: String, val quoted: Boolean = false) : YamlValue
    data class Seq(val items: List<YamlValue>) : YamlValue
    data class Map(val entries: List<Pair<String, YamlValue>>) : YamlValue
}

class YamlParseException(message: String) : Exception(message)

object Yaml {

    private val ANCHOR = Regex("""(^|[\s:])[&*][A-Za-z0-9_-]+""")

    fun format(text: String, indent: Int = 2): String = emit(parse(text), indent)

    fun parse(text: String): YamlValue {
        val lines = text.lines()
            .map { it.substringBefore(" #").substringBefore("\t#") }
            .mapIndexed { index, raw -> Line(index + 1, raw) }
            .filter { it.content.isNotBlank() && !it.content.trimStart().startsWith("#") }

        for (line in lines) {
            val t = line.content.trimStart()
            when {
                t.startsWith("---") || t.startsWith("...") ->
                    fail(line, "múltiplos documentos não são suportados")
                ANCHOR.containsMatchIn(t) -> fail(line, "âncoras e aliases não são suportados")
                t.startsWith("!") -> fail(line, "tags não são suportadas")
                Regex(""":\s*[|>][-+]?\s*$""").containsMatchIn(t) ->
                    fail(line, "escalares em bloco (| e >) não são suportados")
            }
        }

        if (lines.isEmpty()) return YamlValue.Map(emptyList())
        val expanded = expandInlineSeqMaps(lines)
        return Parser(expanded).parseBlock(expanded.first().indent)
    }

    private fun expandInlineSeqMaps(lines: List<Line>): List<Line> {
        val out = mutableListOf<Line>()
        for (line in lines) {
            val rest = line.trimmed.removePrefix("- ").trim()
            val isSeqItem = line.trimmed.startsWith("- ")
            val opensMap = isSeqItem &&
                !rest.startsWith("{") && !rest.startsWith("[") &&
                Parser(emptyList()).findKeyColon(Line(line.number, rest)) != null
            if (opensMap) {
                out += Line(line.number, " ".repeat(line.indent) + "-")
                out += Line(line.number, " ".repeat(line.indent + 2) + rest)
            } else {
                out += line
            }
        }
        return out
    }

    fun emit(value: YamlValue, indent: Int = 2): String =
        StringBuilder().also { write(it, value, indent, 0, false) }.toString().trimEnd()

    private fun write(sb: StringBuilder, value: YamlValue, indent: Int, depth: Int, inline: Boolean) {
        val pad = " ".repeat(indent * depth)
        when (value) {
            is YamlValue.Scalar -> sb.append(renderScalar(value)).append('\n')
            is YamlValue.Seq -> {
                if (value.items.isEmpty()) { sb.append("[]\n"); return }
                if (inline) sb.append('\n')
                for (item in value.items) {
                    sb.append(pad).append("- ")
                    when (item) {
                        is YamlValue.Scalar -> sb.append(renderScalar(item)).append('\n')
                        else -> {
                            sb.append('\n')
                            write(sb, item, indent, depth + 1, false)
                        }
                    }
                }
            }
            is YamlValue.Map -> {
                if (value.entries.isEmpty()) { sb.append("{}\n"); return }
                if (inline) sb.append('\n')
                for ((key, v) in value.entries) {
                    sb.append(pad).append(key).append(':')
                    when (v) {
                        is YamlValue.Scalar -> sb.append(' ').append(renderScalar(v)).append('\n')
                        is YamlValue.Seq ->
                            if (v.items.isEmpty()) sb.append(" []\n")
                            else write(sb, v, indent, depth + 1, true)
                        is YamlValue.Map ->
                            if (v.entries.isEmpty()) sb.append(" {}\n")
                            else write(sb, v, indent, depth + 1, true)
                    }
                }
            }
        }
    }

    private fun renderScalar(scalar: YamlValue.Scalar): String {
        val t = scalar.text
        val needsQuotes = t.isEmpty() ||
            t.first().isWhitespace() || t.last().isWhitespace() ||
            t.any { it == ':' || it == '#' || it == '{' || it == '}' || it == '[' || it == ']' || it == ',' }
        return if (needsQuotes) "\"" + t.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" else t
    }

    private fun fail(line: Line, reason: String): Nothing =
        throw YamlParseException("YAML inválido na linha ${line.number}: $reason")

    private class Line(val number: Int, val content: String) {
        val indent: Int = content.takeWhile { it == ' ' }.length
        val trimmed: String = content.trim()
    }

    private class Parser(val lines: List<Line>) {
        var pos = 0

        fun parseBlock(indent: Int): YamlValue {
            val first = lines.getOrNull(pos) ?: return YamlValue.Map(emptyList())
            return if (first.trimmed.startsWith("- ") || first.trimmed == "-") {
                parseSeq(indent)
            } else {
                parseMap(indent)
            }
        }

        fun parseMap(indent: Int): YamlValue {
            val entries = mutableListOf<Pair<String, YamlValue>>()
            while (pos < lines.size) {
                val line = lines[pos]
                if (line.indent < indent) break
                if (line.indent > indent) fail(line, "indentação inesperada")
                if (line.trimmed.startsWith("- ")) break

                val colon = findKeyColon(line)
                    ?: fail(line, "esperava 'chave: valor'")
                val key = unquote(line.trimmed.substring(0, colon).trim())
                val rest = line.trimmed.substring(colon + 1).trim()
                pos++

                val value = if (rest.isNotEmpty()) {
                    parseFlowOrScalar(rest, line)
                } else {
                    val next = lines.getOrNull(pos)
                    if (next != null && next.indent > indent) parseBlock(next.indent)
                    else YamlValue.Scalar("")
                }
                entries += key to value
            }
            return YamlValue.Map(entries)
        }

        fun parseSeq(indent: Int): YamlValue {
            val items = mutableListOf<YamlValue>()
            while (pos < lines.size) {
                val line = lines[pos]
                if (line.indent < indent) break
                if (!line.trimmed.startsWith("- ") && line.trimmed != "-") break

                val rest = line.trimmed.removePrefix("-").trim()
                pos++
                items += if (rest.isNotEmpty()) {
                    parseFlowOrScalar(rest, line)
                } else {
                    val next = lines.getOrNull(pos)
                    if (next != null && next.indent > indent) parseBlock(next.indent)
                    else YamlValue.Scalar("")
                }
            }
            return YamlValue.Seq(items)
        }

        fun findKeyColon(line: Line): Int? {
            var quote: Char? = null
            val t = line.trimmed
            for (i in t.indices) {
                val c = t[i]
                when {
                    quote != null -> if (c == quote) quote = null
                    c == '"' || c == '\'' -> quote = c
                    c == ':' && (i == t.lastIndex || t[i + 1] == ' ') -> return i
                }
            }
            return null
        }

        fun parseFlowOrScalar(text: String, line: Line): YamlValue = when {
            text.startsWith("{") -> FlowParser(text, line).parseFlowMap()
            text.startsWith("[") -> FlowParser(text, line).parseFlowSeq()
            else -> YamlValue.Scalar(unquote(text), quoted = text.startsWith("\"") || text.startsWith("'"))
        }
    }

    private class FlowParser(val text: String, val line: Line) {
        var pos = 0

        fun parseFlowMap(): YamlValue {
            expect('{')
            val entries = mutableListOf<Pair<String, YamlValue>>()
            skipSpace()
            if (peek() == '}') { pos++; return YamlValue.Map(entries) }
            while (true) {
                skipSpace()
                val key = unquote(readUntil(':'))
                expect(':')
                skipSpace()
                entries += key.trim() to parseValue()
                skipSpace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> { pos++; return YamlValue.Map(entries) }
                    else -> fail(line, "esperava ',' ou '}' na forma flow")
                }
            }
        }

        fun parseFlowSeq(): YamlValue {
            expect('[')
            val items = mutableListOf<YamlValue>()
            skipSpace()
            if (peek() == ']') { pos++; return YamlValue.Seq(items) }
            while (true) {
                skipSpace()
                items += parseValue()
                skipSpace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> { pos++; return YamlValue.Seq(items) }
                    else -> fail(line, "esperava ',' ou ']' na forma flow")
                }
            }
        }

        fun parseValue(): YamlValue = when (peek()) {
            '{' -> parseFlowMap()
            '[' -> parseFlowSeq()
            else -> {
                val start = pos
                var quote: Char? = null
                while (pos < text.length) {
                    val c = text[pos]
                    if (quote != null) {
                        if (c == quote) quote = null
                    } else when (c) {
                        '"', '\'' -> quote = c
                        ',', '}', ']' -> break
                    }
                    pos++
                }
                YamlValue.Scalar(unquote(text.substring(start, pos).trim()))
            }
        }

        fun readUntil(stop: Char): String {
            val start = pos
            var quote: Char? = null
            while (pos < text.length) {
                val c = text[pos]
                if (quote != null) {
                    if (c == quote) quote = null
                } else {
                    if (c == stop) break
                    if (c == '"' || c == '\'') quote = c
                }
                pos++
            }
            return text.substring(start, pos)
        }

        fun peek(): Char = if (pos < text.length) text[pos] else ' '
        fun skipSpace() { while (pos < text.length && text[pos] == ' ') pos++ }
        fun expect(c: Char) {
            if (peek() != c) fail(line, "esperava '$c' na forma flow")
            pos++
        }
    }

    private fun unquote(text: String): String {
        val t = text.trim()
        return when {
            t.length >= 2 && t.startsWith("\"") && t.endsWith("\"") ->
                t.substring(1, t.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
            t.length >= 2 && t.startsWith("'") && t.endsWith("'") ->
                t.substring(1, t.length - 1).replace("''", "'")
            else -> t
        }
    }
}

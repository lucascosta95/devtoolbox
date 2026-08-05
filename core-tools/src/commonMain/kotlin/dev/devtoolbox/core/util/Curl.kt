package dev.devtoolbox.core.util

object Curl {

    class ParseException(message: String) : Exception(message)

    fun format(command: String, indent: Int = 2): String {
        val tokens = tokenize(command.trim())
        if (tokens.isEmpty()) return ""
        if (tokens.first() != "curl") {
            throw ParseException("o comando precisa começar com 'curl'.")
        }

        val head = mutableListOf("curl")
        val groups = mutableListOf<List<String>>()
        var i = 1
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token.startsWith("-") && takesValue(token) && i + 1 < tokens.size -> {
                    groups += listOf(token, tokens[i + 1])
                    i += 2
                }
                token.startsWith("-") -> { groups += listOf(token); i++ }
                head.size <= 3 && !token.startsWith("-") -> { head += token; i++ }
                else -> { groups += listOf(token); i++ }
            }
        }

        val method = groups.firstOrNull { it.size == 2 && (it[0] == "-X" || it[0] == "--request") }
        if (method != null) {
            groups.remove(method)
            head.add(1, method[0])
            head.add(2, method[1])
        }

        val pad = " ".repeat(indent)
        val sb = StringBuilder(head.joinToString(" ") { quoteIfNeeded(it) })
        for (group in groups) {
            sb.append(" \\\n").append(pad).append(group.joinToString(" ") { quoteIfNeeded(it) })
        }
        return sb.toString()
    }

    private fun takesValue(flag: String) = flag in setOf(
        "-X", "--request", "-H", "--header", "-d", "--data", "--data-raw", "--data-binary",
        "--data-urlencode", "-u", "--user", "-o", "--output", "-A", "--user-agent",
        "-b", "--cookie", "-e", "--referer", "-F", "--form", "--url", "-T", "--upload-file",
        "--connect-timeout", "-m", "--max-time", "--retry", "-w", "--write-out",
    )

    private fun quoteIfNeeded(token: String): String = when {
        token.isEmpty() -> "''"
        token.none { it == ' ' || it == '"' || it == '\'' || it == '{' || it == '&' } -> token
        !token.contains('\'') -> "'$token'"
        else -> "\"" + token.replace("\"", "\\\"") + "\""
    }

    fun tokenize(command: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var hasContent = false
        var i = 0
        while (i < command.length) {
            val c = command[i]
            when {
                quote != null && c == '\\' && quote == '"' && i + 1 < command.length -> {
                    current.append(command[i + 1]); i++
                }
                quote != null && c == quote -> quote = null
                quote != null -> current.append(c)
                c == '"' || c == '\'' -> { quote = c; hasContent = true }
                c == '\\' && i + 1 < command.length && command[i + 1] == '\n' -> i++
                c == '\\' && i + 1 < command.length -> { current.append(command[i + 1]); i++ }
                c.isWhitespace() -> {
                    if (current.isNotEmpty() || hasContent) {
                        tokens += current.toString()
                        current.clear()
                        hasContent = false
                    }
                }
                else -> current.append(c)
            }
            i++
        }
        if (quote != null) throw ParseException("aspas não fechadas no comando.")
        if (current.isNotEmpty() || hasContent) tokens += current.toString()
        return tokens
    }
}

package dev.devtoolbox.core.util

/**
 * Conversão entre as oito caixas do catálogo.
 *
 * A tokenização quebra em espaços, `_`, `-`, `.` e nas fronteiras de camelCase — de forma que
 * qualquer uma das oito saídas possa ser reconvertida em qualquer outra.
 */
object StringCase {

    fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                tokens += current.toString()
                current.clear()
            }
        }

        for ((i, c) in text.withIndex()) {
            when {
                c == ' ' || c == '_' || c == '-' || c == '.' || c == '\t' -> flush()
                // Fronteira camelCase: minúscula/dígito seguida de maiúscula.
                c.isUpperCase() && i > 0 && (text[i - 1].isLowerCase() || text[i - 1].isDigit()) -> {
                    flush()
                    current.append(c)
                }
                // Fim de uma sigla: "HTTPServer" → "HTTP" + "Server".
                c.isUpperCase() && i > 0 && text[i - 1].isUpperCase() &&
                    i + 1 < text.length && text[i + 1].isLowerCase() -> {
                    flush()
                    current.append(c)
                }
                else -> current.append(c)
            }
        }
        flush()
        return tokens.filter { it.isNotEmpty() }
    }

    fun lower(text: String) = tokenize(text).joinToString(" ") { it.lowercase() }
    fun upper(text: String) = tokenize(text).joinToString(" ") { it.uppercase() }
    fun capitalized(text: String) = tokenize(text).joinToString(" ") { it.capitalizeFirst() }
    fun camel(text: String) = tokenize(text).mapIndexed { i, t ->
        if (i == 0) t.lowercase() else t.capitalizeFirst()
    }.joinToString("")
    fun pascal(text: String) = tokenize(text).joinToString("") { it.capitalizeFirst() }
    fun snake(text: String) = tokenize(text).joinToString("_") { it.lowercase() }
    fun kebab(text: String) = tokenize(text).joinToString("-") { it.lowercase() }
    fun constant(text: String) = tokenize(text).joinToString("_") { it.uppercase() }

    private fun String.capitalizeFirst(): String =
        if (isEmpty()) this else first().uppercase() + substring(1).lowercase()
}

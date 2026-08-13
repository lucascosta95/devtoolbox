package dev.devtoolbox.core.util

object JsonString {

    fun unwrap(text: String): String {
        val trimmed = text.trim()
        return when {
            trimmed.startsWith("\"") -> asJsonString(trimmed)
            trimmed.contains("\\\"") -> asJsonString("\"" + trimmed + "\"")
            else -> trimmed
        }
    }

    fun format(text: String, indent: Int = 2): String = Json.format(unwrap(text), indent)

    private fun asJsonString(literal: String): String {
        val value = Json.parse(literal)
        if (value !is JsonValue.Str) {
            throw JsonParseException("a entrada não é uma string JSON")
        }
        return value.value
    }
}

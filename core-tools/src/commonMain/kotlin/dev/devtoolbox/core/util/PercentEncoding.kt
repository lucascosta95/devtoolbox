package dev.devtoolbox.core.util

object PercentEncoding {

    class DecodeException(message: String) : Exception(message)

    private fun Char.isUnreserved() =
        this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' ||
            this == '-' || this == '.' || this == '_' || this == '~'

    fun encode(text: String): String {
        val sb = StringBuilder(text.length)
        for (byte in text.encodeToByteArray()) {
            val c = (byte.toInt() and 0xFF).toChar()
            if (c.isUnreserved()) {
                sb.append(c)
            } else {
                sb.append('%').append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
            }
        }
        return sb.toString()
    }

    fun decode(text: String): String {
        val out = ArrayList<Byte>(text.length)
        var i = 0
        while (i < text.length) {
            when (val c = text[i]) {
                '%' -> {
                    if (i + 2 >= text.length) {
                        throw DecodeException("sequência '%' incompleta na posição ${i + 1}")
                    }
                    val hex = text.substring(i + 1, i + 3)
                    val v = hex.toIntOrNull(16)
                        ?: throw DecodeException("sequência '%$hex' não é hexadecimal válida")
                    out.add(v.toByte())
                    i += 3
                }
                '+' -> { out.add(' '.code.toByte()); i++ }
                else -> {
                    for (b in c.toString().encodeToByteArray()) out.add(b)
                    i++
                }
            }
        }
        return out.toByteArray().decodeToString()
    }

    fun looksEncoded(text: String): Boolean =
        Regex("%[0-9a-fA-F]{2}").containsMatchIn(text)
}

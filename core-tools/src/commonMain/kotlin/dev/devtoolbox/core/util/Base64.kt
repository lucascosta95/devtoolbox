package dev.devtoolbox.core.util

object Base64 {

    private const val STANDARD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private const val URL_SAFE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    class DecodeException(message: String) : Exception(message)

    fun encode(text: String, urlSafe: Boolean = false, padded: Boolean = true): String =
        encodeBytes(text.encodeToByteArray(), urlSafe, padded)

    fun encodeBytes(bytes: ByteArray, urlSafe: Boolean = false, padded: Boolean = true): String {
        val alphabet = if (urlSafe) URL_SAFE else STANDARD
        val sb = StringBuilder((bytes.size + 2) / 3 * 4)
        var i = 0
        while (i + 2 < bytes.size) {
            val n = (bytes[i].toInt() and 0xFF shl 16) or
                (bytes[i + 1].toInt() and 0xFF shl 8) or
                (bytes[i + 2].toInt() and 0xFF)
            sb.append(alphabet[n ushr 18 and 0x3F])
            sb.append(alphabet[n ushr 12 and 0x3F])
            sb.append(alphabet[n ushr 6 and 0x3F])
            sb.append(alphabet[n and 0x3F])
            i += 3
        }
        when (bytes.size - i) {
            1 -> {
                val n = bytes[i].toInt() and 0xFF shl 16
                sb.append(alphabet[n ushr 18 and 0x3F])
                sb.append(alphabet[n ushr 12 and 0x3F])
                if (padded) sb.append("==")
            }
            2 -> {
                val n = (bytes[i].toInt() and 0xFF shl 16) or (bytes[i + 1].toInt() and 0xFF shl 8)
                sb.append(alphabet[n ushr 18 and 0x3F])
                sb.append(alphabet[n ushr 12 and 0x3F])
                sb.append(alphabet[n ushr 6 and 0x3F])
                if (padded) sb.append('=')
            }
        }
        return sb.toString()
    }

    fun decodeToBytes(text: String): ByteArray {
        val clean = text.filterNot { it == '\n' || it == '\r' || it == ' ' || it == '\t' }
            .trimEnd('=')
        val out = ArrayList<Byte>(clean.length * 3 / 4 + 3)
        var buffer = 0
        var bits = 0
        for (c in clean) {
            val v = value(c) ?: throw DecodeException("caractere inválido '$c' para Base64")
            buffer = buffer shl 6 or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.add((buffer ushr bits and 0xFF).toByte())
            }
        }
        if (bits >= 6) throw DecodeException("comprimento inválido para Base64")
        return out.toByteArray()
    }

    fun decode(text: String): String = decodeToBytes(text).decodeToString()

    fun looksLikeBase64(text: String): Boolean {
        val clean = text.filterNot { it == '\n' || it == '\r' || it == ' ' || it == '\t' }
        if (clean.length < 4 || clean.any { value(it) == null && it != '=' }) return false
        return runCatching { decodeToBytes(clean) }.isSuccess
    }

    private fun value(c: Char): Int? = when (c) {
        in 'A'..'Z' -> c - 'A'
        in 'a'..'z' -> c - 'a' + 26
        in '0'..'9' -> c - '0' + 52
        '+', '-' -> 62
        '/', '_' -> 63
        else -> null
    }
}

package dev.devtoolbox.core.util

/**
 * MD5, SHA-1 e SHA-256 em Kotlin puro.
 *
 * Escritos à mão para manter [dev.devtoolbox.core] 100% `commonMain` — `java.security` só
 * existiria no target JVM. Conferidos contra os vetores oficiais em `DigestTest`.
 *
 * MD5 e SHA-1 estão aqui porque a ferramenta os expõe para inspeção de valores legados;
 * nenhum dos dois deve ser usado para segurança.
 */
object Digest {

    fun md5(text: String): String = md5(text.encodeToByteArray()).toHex()

    fun sha1(text: String): String = sha1(text.encodeToByteArray()).toHex()

    fun sha256(text: String): String = sha256(text.encodeToByteArray()).toHex()

    // ---------------------------------------------------------------- MD5

    private val MD5_S = intArrayOf(
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
    )

    /** K[i] = floor(2^32 × |sin(i + 1)|) — tabela da RFC 1321. */
    private val MD5_K = intArrayOf(
        -0x28955b88, -0x173848aa, 0x242070db, -0x3e423112,
        -0xa83f051, 0x4787c62a, -0x57cfb9ed, -0x2b96aff,
        0x698098d8, -0x74bb0851, -0xa44f, -0x76a32842,
        0x6b901122, -0x2678e6d, -0x5986bc72, 0x49b40821,
        -0x9e1da9e, -0x3fbf4cc0, 0x265e5a51, -0x16493856,
        -0x29d0efa3, 0x02441453, -0x275e197f, -0x182c0438,
        0x21e1cde6, -0x3cc8f82a, -0xb2af279, 0x455a14ed,
        -0x561c16fb, -0x3105c08, 0x676f02d9, -0x72d5b376,
        -0x5c6be, -0x788e097f, 0x6d9d6122, -0x21ac7f4,
        -0x5b4115bc, 0x4bdecfa9, -0x944b4a0, -0x41404390,
        0x289b7ec6, -0x155ed806, -0x2b10cf7b, 0x04881d05,
        -0x262b2fc7, -0x1924661b, 0x1fa27cf8, -0x3b53a99b,
        -0xbd6ddbc, 0x432aff97, -0x546bdc59, -0x36c5fc7,
        0x655b59c3, -0x70f3336e, -0x100b83, -0x7a7ba22f,
        0x6fa87e4f, -0x1d31920, -0x5cfebcec, 0x4e0811a1,
        -0x8ac817e, -0x42c50dcb, 0x2ad7d2bb, -0x14792c6f,
    )

    fun md5(bytes: ByteArray): ByteArray {
        var a0 = 0x67452301
        var b0 = -0x10325477
        var c0 = -0x67452302
        var d0 = 0x10325476

        // MD5 usa comprimento little-endian, ao contrário de SHA.
        for (chunk in pad(bytes, littleEndianLength = true)) {
            val m = IntArray(16) { i -> chunk.leInt(i * 4) }
            var a = a0
            var b = b0
            var c = c0
            var d = d0
            for (i in 0 until 64) {
                val (f, g) = when (i / 16) {
                    0 -> ((b and c) or (b.inv() and d)) to i
                    1 -> ((d and b) or (d.inv() and c)) to (5 * i + 1) % 16
                    2 -> (b xor c xor d) to (3 * i + 5) % 16
                    else -> (c xor (b or d.inv())) to (7 * i) % 16
                }
                val tmp = d
                d = c
                c = b
                b = b + ((a + f + MD5_K[i] + m[g]).rotateLeft(MD5_S[i]))
                a = tmp
            }
            a0 += a; b0 += b; c0 += c; d0 += d
        }
        return byteArrayOf(*a0.leBytes(), *b0.leBytes(), *c0.leBytes(), *d0.leBytes())
    }

    // --------------------------------------------------------------- SHA-1

    fun sha1(bytes: ByteArray): ByteArray {
        var h0 = 0x67452301
        var h1 = -0x10325477
        var h2 = -0x67452302
        var h3 = 0x10325476
        var h4 = -0x3c2d1e10

        for (chunk in pad(bytes, littleEndianLength = false)) {
            val w = IntArray(80)
            for (i in 0 until 16) w[i] = chunk.beInt(i * 4)
            for (i in 16 until 80) {
                w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
            }
            var a = h0; var b = h1; var c = h2; var d = h3; var e = h4
            for (i in 0 until 80) {
                val (f, k) = when (i / 20) {
                    0 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                    1 -> (b xor c xor d) to 0x6ED9EBA1
                    2 -> ((b and c) or (b and d) or (c and d)) to -0x70e44324
                    else -> (b xor c xor d) to -0x359d3e2a
                }
                val temp = a.rotateLeft(5) + f + e + k + w[i]
                e = d; d = c; c = b.rotateLeft(30); b = a; a = temp
            }
            h0 += a; h1 += b; h2 += c; h3 += d; h4 += e
        }
        return byteArrayOf(*h0.beBytes(), *h1.beBytes(), *h2.beBytes(), *h3.beBytes(), *h4.beBytes())
    }

    // ------------------------------------------------------------- SHA-256

    private val SHA256_K = intArrayOf(
        0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b, 0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
        -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
        -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039, -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
        -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d, -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8, -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e,
    )

    fun sha256(bytes: ByteArray): ByteArray {
        val h = intArrayOf(
            0x6a09e667, -0x4498517b, 0x3c6ef372, -0x5ab00ac6,
            0x510e527f, -0x64fa9774, 0x1f83d9ab, 0x5be0cd19,
        )

        for (chunk in pad(bytes, littleEndianLength = false)) {
            val w = IntArray(64)
            for (i in 0 until 16) w[i] = chunk.beInt(i * 4)
            for (i in 16 until 64) {
                val s0 = w[i - 15].rotateRight(7) xor w[i - 15].rotateRight(18) xor (w[i - 15] ushr 3)
                val s1 = w[i - 2].rotateRight(17) xor w[i - 2].rotateRight(19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }
            var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
            var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
            for (i in 0 until 64) {
                val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = hh + s1 + ch + SHA256_K[i] + w[i]
                val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj
                hh = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }
            h[0] += a; h[1] += b; h[2] += c; h[3] += d
            h[4] += e; h[5] += f; h[6] += g; h[7] += hh
        }
        return ByteArray(32) { i -> (h[i / 4] ushr (24 - 8 * (i % 4))).toByte() }
    }

    // --------------------------------------------------------------- comum

    /** Padding de Merkle–Damgård compartilhado por MD5, SHA-1 e SHA-256. */
    private fun pad(bytes: ByteArray, littleEndianLength: Boolean): List<ByteArray> {
        val bitLength = bytes.size.toLong() * 8
        val paddedSize = ((bytes.size + 8) / 64 + 1) * 64
        val padded = ByteArray(paddedSize)
        bytes.copyInto(padded)
        padded[bytes.size] = 0x80.toByte()
        for (i in 0 until 8) {
            val shift = 8 * i
            val byte = (bitLength ushr shift).toByte()
            padded[paddedSize - if (littleEndianLength) 8 - i else 1 + i] = byte
        }
        return (0 until paddedSize / 64).map { padded.copyOfRange(it * 64, (it + 1) * 64) }
    }

    private fun ByteArray.beInt(offset: Int): Int =
        (this[offset].toInt() and 0xFF shl 24) or
            (this[offset + 1].toInt() and 0xFF shl 16) or
            (this[offset + 2].toInt() and 0xFF shl 8) or
            (this[offset + 3].toInt() and 0xFF)

    private fun ByteArray.leInt(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or
            (this[offset + 1].toInt() and 0xFF shl 8) or
            (this[offset + 2].toInt() and 0xFF shl 16) or
            (this[offset + 3].toInt() and 0xFF shl 24)

    private fun Int.beBytes() = byteArrayOf(
        (this ushr 24).toByte(), (this ushr 16).toByte(), (this ushr 8).toByte(), toByte(),
    )

    private fun Int.leBytes() = byteArrayOf(
        toByte(), (this ushr 8).toByte(), (this ushr 16).toByte(), (this ushr 24).toByte(),
    )
}

fun ByteArray.toHex(): String {
    val hex = "0123456789abcdef"
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        sb.append(hex[v ushr 4]).append(hex[v and 0x0F])
    }
    return sb.toString()
}

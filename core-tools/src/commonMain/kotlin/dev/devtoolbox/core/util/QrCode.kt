package dev.devtoolbox.core.util

class QrEncodeException(message: String) : Exception(message)

object QrCode {
    private val BYTE_CAPACITY_M = intArrayOf(14, 26, 42, 62, 84, 106, 122, 152, 180, 213)

    private val DATA_CODEWORDS_M = intArrayOf(16, 28, 44, 64, 86, 108, 124, 154, 182, 216)

    private val BLOCKS_M: Array<IntArray> = arrayOf(
        intArrayOf(1, 16),
        intArrayOf(1, 28),
        intArrayOf(1, 44),
        intArrayOf(2, 32),
        intArrayOf(2, 43),
        intArrayOf(4, 27),
        intArrayOf(4, 31),
        intArrayOf(2, 38, 2, 39),
        intArrayOf(3, 36, 2, 37),
        intArrayOf(4, 43, 1, 44),
    )

    private val EC_PER_BLOCK_M = intArrayOf(10, 16, 26, 18, 24, 16, 18, 22, 22, 26)

    private val ALIGNMENT_CENTERS: Array<IntArray> = arrayOf(
        intArrayOf(),
        intArrayOf(6, 18),
        intArrayOf(6, 22),
        intArrayOf(6, 26),
        intArrayOf(6, 30),
        intArrayOf(6, 34),
        intArrayOf(6, 22, 38),
        intArrayOf(6, 24, 42),
        intArrayOf(6, 26, 46),
        intArrayOf(6, 28, 50),
    )

    fun encode(text: String): List<List<Boolean>> {
        val data = text.encodeToByteArray()
        val version = (1..10).firstOrNull { data.size <= BYTE_CAPACITY_M[it - 1] }
            ?: throw QrEncodeException(
                "texto longo demais (${data.size} bytes) — o limite é ${BYTE_CAPACITY_M.last()}.",
            )

        val bits = buildDataBits(data, version)
        val codewords = interleave(bits, version)
        val size = 17 + 4 * version

        val modules = Array(size) { arrayOfNulls<Boolean>(size) }
        placeFunctionPatterns(modules, version)
        val functionModules = Array(size) { row -> BooleanArray(size) { col -> modules[row][col] != null } }
        placeCodewords(modules, codewords)

        var best: Array<Array<Boolean?>>? = null
        var bestPenalty = Int.MAX_VALUE
        for (mask in 0..7) {
            val candidate = modules.deepCopy()
            applyMask(candidate, mask, functionModules)
            placeFormatInfo(candidate, mask)
            val penalty = penalty(candidate)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                best = candidate
            }
        }

        val result = best!!
        return result.map { row -> row.map { it == true } }
    }

    private fun buildDataBits(data: ByteArray, version: Int): BitBuffer {
        val bits = BitBuffer()
        bits.append(0b0100, 4)
        bits.append(data.size, if (version <= 9) 8 else 16)
        for (b in data) bits.append(b.toInt() and 0xFF, 8)

        val capacityBits = DATA_CODEWORDS_M[version - 1] * 8
        if (bits.size > capacityBits) {
            throw QrEncodeException("texto não cabe na versão $version.")
        }

        repeat(minOf(4, capacityBits - bits.size)) { bits.append(0, 1) }
        while (bits.size % 8 != 0) bits.append(0, 1)
        var pad = 0xEC
        while (bits.size < capacityBits) {
            bits.append(pad, 8)
            pad = if (pad == 0xEC) 0x11 else 0xEC
        }
        return bits
    }

    private fun interleave(bits: BitBuffer, version: Int): IntArray {
        val layout = BLOCKS_M[version - 1]
        val ecCount = EC_PER_BLOCK_M[version - 1]
        val all = bits.toBytes()

        val blocks = mutableListOf<IntArray>()
        var offset = 0
        var i = 0
        while (i < layout.size) {
            val count = layout[i]
            val length = layout[i + 1]
            repeat(count) {
                blocks += IntArray(length) { k -> all[offset + k] }
                offset += length
            }
            i += 2
        }

        val ecBlocks = blocks.map { ReedSolomon.encode(it, ecCount) }

        val out = mutableListOf<Int>()
        val maxData = blocks.maxOf { it.size }
        for (col in 0 until maxData) {
            for (block in blocks) if (col < block.size) out += block[col]
        }
        for (col in 0 until ecCount) {
            for (block in ecBlocks) out += block[col]
        }
        return out.toIntArray()
    }

    private fun placeFunctionPatterns(m: Array<Array<Boolean?>>, version: Int) {
        val size = m.size

        fun finder(row: Int, col: Int) {
            for (r in -1..7) for (c in -1..7) {
                val rr = row + r
                val cc = col + c
                if (rr !in 0 until size || cc !in 0 until size) continue
                val inside = r in 0..6 && c in 0..6
                val border = (r == 0 || r == 6) && c in 0..6 || (c == 0 || c == 6) && r in 0..6
                val core = r in 2..4 && c in 2..4
                m[rr][cc] = inside && (border || core)
            }
        }
        finder(0, 0)
        finder(0, size - 7)
        finder(size - 7, 0)

        for (i in 8 until size - 8) {
            m[6][i] = i % 2 == 0
            m[i][6] = i % 2 == 0
        }

        val centers = ALIGNMENT_CENTERS[version - 1]
        for (r in centers) for (c in centers) {
            val nearFinder = (r <= 8 && c <= 8) || (r <= 8 && c >= size - 9) || (r >= size - 9 && c <= 8)
            if (nearFinder) continue
            for (dr in -2..2) for (dc in -2..2) {
                m[r + dr][c + dc] = dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0)
            }
        }

        m[size - 8][8] = true
        for (i in 0..8) {
            if (m[8][i] == null) m[8][i] = false
            if (m[i][8] == null) m[i][8] = false
        }
        for (i in 0..7) {
            if (m[8][size - 1 - i] == null) m[8][size - 1 - i] = false
            if (m[size - 1 - i][8] == null) m[size - 1 - i][8] = false
        }
    }

    private fun placeCodewords(m: Array<Array<Boolean?>>, codewords: IntArray) {
        val size = m.size
        val bits = BitBuffer().also { for (cw in codewords) it.append(cw, 8) }
        var bitIndex = 0
        var upward = true
        var col = size - 1

        while (col > 0) {
            if (col == 6) col--
            val rows = if (upward) (size - 1) downTo 0 else 0 until size
            for (row in rows) {
                for (c in listOf(col, col - 1)) {
                    if (m[row][c] != null) continue
                    m[row][c] = if (bitIndex < bits.size) bits[bitIndex] else false
                    bitIndex++
                }
            }
            upward = !upward
            col -= 2
        }
    }

    private fun applyMask(m: Array<Array<Boolean?>>, mask: Int, functionModules: Array<BooleanArray>) {
        val size = m.size
        for (row in 0 until size) for (col in 0 until size) {
            if (functionModules[row][col]) continue
            val flip = when (mask) {
                0 -> (row + col) % 2 == 0
                1 -> row % 2 == 0
                2 -> col % 3 == 0
                3 -> (row + col) % 3 == 0
                4 -> (row / 2 + col / 3) % 2 == 0
                5 -> (row * col) % 2 + (row * col) % 3 == 0
                6 -> ((row * col) % 2 + (row * col) % 3) % 2 == 0
                else -> ((row + col) % 2 + (row * col) % 3) % 2 == 0
            }
            if (flip) m[row][col] = m[row][col] != true
        }
    }

    fun formatBits(mask: Int): Int {
        val data = (0b00 shl 3) or mask
        var value = data shl 10
        val generator = 0b10100110111
        for (i in 14 downTo 10) {
            if ((value shr i) and 1 == 1) value = value xor (generator shl (i - 10))
        }
        return ((data shl 10) or value) xor 0b101010000010010
    }

    private fun placeFormatInfo(m: Array<Array<Boolean?>>, mask: Int) {
        val size = m.size
        val format = formatBits(mask)
        for (i in 0..14) {
            val bit = (format shr (14 - i)) and 1 == 1
            when {
                i < 6 -> m[8][i] = bit
                i == 6 -> m[8][7] = bit
                i == 7 -> m[8][8] = bit
                i == 8 -> m[7][8] = bit
                else -> m[14 - i][8] = bit
            }
            if (i < 7) m[size - 1 - i][8] = bit else m[8][size - 8 + (i - 7)] = bit
        }
        m[size - 8][8] = true
    }

    private fun penalty(m: Array<Array<Boolean?>>): Int {
        val size = m.size
        val g = Array(size) { r -> BooleanArray(size) { c -> m[r][c] == true } }
        var score = 0

        for (i in 0 until size) {
            score += runPenalty(BooleanArray(size) { g[i][it] })
            score += runPenalty(BooleanArray(size) { g[it][i] })
        }
        for (r in 0 until size - 1) for (c in 0 until size - 1) {
            if (g[r][c] == g[r][c + 1] && g[r][c] == g[r + 1][c] && g[r][c] == g[r + 1][c + 1]) {
                score += 3
            }
        }
        val pattern = booleanArrayOf(true, false, true, true, true, false, true, false, false, false, false)
        for (r in 0 until size) for (c in 0 until size - 10) {
            if ((0..10).all { g[r][c + it] == pattern[it] }) score += 40
            if ((0..10).all { g[r][c + it] == pattern[10 - it] }) score += 40
        }
        for (c in 0 until size) for (r in 0 until size - 10) {
            if ((0..10).all { g[r + it][c] == pattern[it] }) score += 40
            if ((0..10).all { g[r + it][c] == pattern[10 - it] }) score += 40
        }
        val dark = g.sumOf { row -> row.count { it } }
        val percent = dark * 100 / (size * size)
        score += 10 * (kotlin.math.abs(percent - 50) / 5)
        return score
    }

    private fun runPenalty(line: BooleanArray): Int {
        var score = 0
        var run = 1
        for (i in 1 until line.size) {
            if (line[i] == line[i - 1]) {
                run++
            } else {
                if (run >= 5) score += 3 + (run - 5)
                run = 1
            }
        }
        if (run >= 5) score += 3 + (run - 5)
        return score
    }

    private fun Array<Array<Boolean?>>.deepCopy() = Array(size) { this[it].copyOf() }
}

class BitBuffer {
    private val bits = mutableListOf<Boolean>()

    val size: Int get() = bits.size

    operator fun get(index: Int): Boolean = bits[index]

    fun append(value: Int, length: Int) {
        for (i in length - 1 downTo 0) bits += (value shr i) and 1 == 1
    }

    fun toBytes(): IntArray {
        val out = IntArray((bits.size + 7) / 8)
        for ((i, bit) in bits.withIndex()) {
            if (bit) out[i / 8] = out[i / 8] or (1 shl (7 - i % 8))
        }
        return out
    }
}

object ReedSolomon {

    private val exp = IntArray(512)
    private val log = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            log[x] = i
            x = x shl 1
            if (x and 0x100 != 0) x = x xor 0x11D
        }
        for (i in 255 until 512) exp[i] = exp[i - 255]
    }

    fun multiply(a: Int, b: Int): Int =
        if (a == 0 || b == 0) 0 else exp[log[a] + log[b]]

    fun generator(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor multiply(poly[j], exp[i])
            }
            poly = next
        }
        return poly
    }

    fun encode(data: IntArray, ecCount: Int): IntArray {
        val gen = generator(ecCount)
        val remainder = IntArray(data.size + ecCount)
        data.copyInto(remainder)
        for (i in data.indices) {
            val factor = remainder[i]
            if (factor == 0) continue
            for (j in gen.indices) {
                remainder[i + j] = remainder[i + j] xor multiply(gen[j], factor)
            }
        }
        return remainder.copyOfRange(data.size, remainder.size)
    }
}

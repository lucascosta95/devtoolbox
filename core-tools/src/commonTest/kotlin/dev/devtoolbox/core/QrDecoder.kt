package dev.devtoolbox.core

object QrDecoder {

    class DecodeException(message: String) : Exception(message)

    private val DATA_CODEWORDS_M = intArrayOf(16, 28, 44, 64, 86, 108, 124, 154, 182, 216)
    private val EC_PER_BLOCK_M = intArrayOf(10, 16, 26, 18, 24, 16, 18, 22, 22, 26)
    private val BLOCKS_M: Array<IntArray> = arrayOf(
        intArrayOf(1, 16), intArrayOf(1, 28), intArrayOf(1, 44), intArrayOf(2, 32),
        intArrayOf(2, 43), intArrayOf(4, 27), intArrayOf(4, 31), intArrayOf(2, 38, 2, 39),
        intArrayOf(3, 36, 2, 37), intArrayOf(4, 43, 1, 44),
    )
    private val ALIGNMENT_CENTERS: Array<IntArray> = arrayOf(
        intArrayOf(), intArrayOf(6, 18), intArrayOf(6, 22), intArrayOf(6, 26), intArrayOf(6, 30),
        intArrayOf(6, 34), intArrayOf(6, 22, 38), intArrayOf(6, 24, 42), intArrayOf(6, 26, 46),
        intArrayOf(6, 28, 50),
    )

    fun functionModuleCount(size: Int, version: Int): Int =
        functionMap(size, version).sumOf { row -> row.count { it } }

    fun decode(modules: List<List<Boolean>>): String {
        val size = modules.size
        if ((size - 17) % 4 != 0) throw DecodeException("tamanho $size não corresponde a nenhuma versão")
        val version = (size - 17) / 4
        if (version !in 1..10) throw DecodeException("versão $version fora do suportado")

        val mask = readMask(modules)
        val function = functionMap(size, version)

        val unmasked = Array(size) { row ->
            BooleanArray(size) { col ->
                val bit = modules[row][col]
                if (function[row][col]) bit else bit != maskFlip(mask, row, col)
            }
        }

        val codewords = readCodewords(unmasked, function)
        val data = deinterleave(codewords, version)
        return readPayload(data, version)
    }

    private fun readMask(m: List<List<Boolean>>): Int {
        var format = 0
        for (i in 0..14) {
            val bit = when {
                i < 6 -> m[8][i]
                i == 6 -> m[8][7]
                i == 7 -> m[8][8]
                i == 8 -> m[7][8]
                else -> m[14 - i][8]
            }
            if (bit) format = format or (1 shl (14 - i))
        }
        val unmasked = format xor 0b101010000010010
        val ecLevel = (unmasked shr 13) and 0b11
        if (ecLevel != 0b00) throw DecodeException("nível de correção lido não é M (bits $ecLevel)")
        return (unmasked shr 10) and 0b111
    }

    private fun maskFlip(mask: Int, row: Int, col: Int): Boolean = when (mask) {
        0 -> (row + col) % 2 == 0
        1 -> row % 2 == 0
        2 -> col % 3 == 0
        3 -> (row + col) % 3 == 0
        4 -> (row / 2 + col / 3) % 2 == 0
        5 -> (row * col) % 2 + (row * col) % 3 == 0
        6 -> ((row * col) % 2 + (row * col) % 3) % 2 == 0
        else -> ((row + col) % 2 + (row * col) % 3) % 2 == 0
    }

    private fun functionMap(size: Int, version: Int): Array<BooleanArray> {
        val f = Array(size) { BooleanArray(size) }
        fun block(row: Int, col: Int, h: Int, w: Int) {
            for (r in row until row + h) for (c in col until col + w) {
                if (r in 0 until size && c in 0 until size) f[r][c] = true
            }
        }
        block(0, 0, 9, 9)
        block(0, size - 8, 9, 8)
        block(size - 8, 0, 8, 9)
        for (i in 0 until size) { f[6][i] = true; f[i][6] = true }
        val centers = ALIGNMENT_CENTERS[version - 1]
        for (r in centers) for (c in centers) {
            val nearFinder = (r <= 8 && c <= 8) || (r <= 8 && c >= size - 9) || (r >= size - 9 && c <= 8)
            if (nearFinder) continue
            block(r - 2, c - 2, 5, 5)
        }
        return f
    }

    private fun readCodewords(m: Array<BooleanArray>, function: Array<BooleanArray>): IntArray {
        val size = m.size
        val bits = mutableListOf<Boolean>()
        var upward = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col--
            val rows = if (upward) (size - 1) downTo 0 else 0 until size
            for (row in rows) {
                for (c in listOf(col, col - 1)) {
                    if (function[row][c]) continue
                    bits += m[row][c]
                }
            }
            upward = !upward
            col -= 2
        }
        return IntArray(bits.size / 8) { i ->
            var v = 0
            for (b in 0 until 8) if (bits[i * 8 + b]) v = v or (1 shl (7 - b))
            v
        }
    }

    private fun deinterleave(codewords: IntArray, version: Int): IntArray {
        val layout = BLOCKS_M[version - 1]
        val ecCount = EC_PER_BLOCK_M[version - 1]

        val lengths = mutableListOf<Int>()
        var i = 0
        while (i < layout.size) {
            repeat(layout[i]) { lengths += layout[i + 1] }
            i += 2
        }

        val blocks = lengths.map { IntArray(it) }
        var index = 0
        for (col in 0 until lengths.max()) {
            for (block in blocks) {
                if (col < block.size) block[col] = codewords[index++]
            }
        }
        val expected = DATA_CODEWORDS_M[version - 1]
        val out = blocks.flatMap { it.toList() }.toIntArray()
        if (out.size != expected) throw DecodeException("esperava $expected codewords, li ${out.size}")
        if (index + ecCount * blocks.size > codewords.size) {
            throw DecodeException("faltam codewords de correção na matriz")
        }
        return out
    }

    private fun readPayload(data: IntArray, version: Int): String {
        val mode = data[0] shr 4
        if (mode != 0b0100) throw DecodeException("modo $mode não é byte")

        val countBits = if (version <= 9) 8 else 16
        var bitPos = 4
        fun read(n: Int): Int {
            var v = 0
            repeat(n) {
                val byte = data[bitPos / 8]
                val bit = (byte shr (7 - bitPos % 8)) and 1
                v = (v shl 1) or bit
                bitPos++
            }
            return v
        }
        val length = read(countBits)
        val bytes = ByteArray(length) { read(8).toByte() }
        return bytes.decodeToString()
    }
}

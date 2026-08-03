package dev.devtoolbox.core.util

/**
 * Encoder de QR Code (ISO/IEC 18004), modo *byte*, nível de correção **M**, versões 1–10.
 *
 * Escrito à mão porque as libs de QR para KMP (qrose) dependem de Compose UI — e o core
 * precisa ficar livre de UI. Cobre o caso de uso da ferramenta: texto e URLs curtas.
 */
class QrEncodeException(message: String) : Exception(message)

object QrCode {

    /** Capacidade em bytes por versão (1–10), modo byte, nível M. */
    private val BYTE_CAPACITY_M = intArrayOf(14, 26, 42, 62, 84, 106, 122, 152, 180, 213)

    /** Total de codewords de dados por versão (1–10), nível M. */
    private val DATA_CODEWORDS_M = intArrayOf(16, 28, 44, 64, 86, 108, 124, 154, 182, 216)

    /** (blocos do grupo 1, codewords por bloco) por versão, nível M. Versões com 2 grupos listam ambos. */
    private val BLOCKS_M: Array<IntArray> = arrayOf(
        intArrayOf(1, 16),                 // v1: 1 bloco de 16
        intArrayOf(1, 28),                 // v2
        intArrayOf(1, 44),                 // v3
        intArrayOf(2, 32),                 // v4: 2×32
        intArrayOf(2, 43),                 // v5: 2×43
        intArrayOf(4, 27),                 // v6: 4×27
        intArrayOf(4, 31),                 // v7
        intArrayOf(2, 38, 2, 39),          // v8: 2×38 + 2×39
        intArrayOf(3, 36, 2, 37),          // v9
        intArrayOf(4, 43, 1, 44),          // v10
    )

    /** Codewords de correção por bloco, nível M, versões 1–10. */
    private val EC_PER_BLOCK_M = intArrayOf(10, 16, 26, 18, 24, 16, 18, 22, 22, 26)

    /** Centros dos padrões de alinhamento por versão (vazio na v1). */
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

    /**
     * Codifica [text] e devolve a matriz de módulos (`true` = escuro), sem zona de silêncio.
     */
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
        // Tudo que já está preenchido neste ponto é módulo de função — inclusive os padrões
        // de alinhamento e as áreas reservadas de formato. Nada disso pode receber máscara.
        val functionModules = Array(size) { row -> BooleanArray(size) { col -> modules[row][col] != null } }
        placeCodewords(modules, codewords)

        // Escolhe a máscara com a menor penalidade, como manda a norma.
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

    // ------------------------------------------------------------ bitstream

    private fun buildDataBits(data: ByteArray, version: Int): BitBuffer {
        val bits = BitBuffer()
        bits.append(0b0100, 4)                                   // modo byte
        bits.append(data.size, if (version <= 9) 8 else 16)      // contador de caracteres
        for (b in data) bits.append(b.toInt() and 0xFF, 8)

        val capacityBits = DATA_CODEWORDS_M[version - 1] * 8
        if (bits.size > capacityBits) {
            throw QrEncodeException("texto não cabe na versão $version.")
        }

        // Terminador de até 4 bits, alinhamento de byte e bytes de preenchimento alternados.
        repeat(minOf(4, capacityBits - bits.size)) { bits.append(0, 1) }
        while (bits.size % 8 != 0) bits.append(0, 1)
        var pad = 0xEC
        while (bits.size < capacityBits) {
            bits.append(pad, 8)
            pad = if (pad == 0xEC) 0x11 else 0xEC
        }
        return bits
    }

    /** Divide em blocos, calcula a correção de erros e intercala como manda a norma. */
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

    // ------------------------------------------------------------- matriz

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

        // Padrões de tempo.
        for (i in 8 until size - 8) {
            m[6][i] = i % 2 == 0
            m[i][6] = i % 2 == 0
        }

        // Padrões de alinhamento, exceto onde colidem com os localizadores.
        val centers = ALIGNMENT_CENTERS[version - 1]
        for (r in centers) for (c in centers) {
            val nearFinder = (r <= 8 && c <= 8) || (r <= 8 && c >= size - 9) || (r >= size - 9 && c <= 8)
            if (nearFinder) continue
            for (dr in -2..2) for (dc in -2..2) {
                m[r + dr][c + dc] = dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0)
            }
        }

        // Módulo escuro fixo e reserva das áreas de formato.
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
            if (col == 6) col-- // a coluna 6 é o padrão de tempo vertical
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

    /** Informação de formato: nível M + máscara, com BCH(15,5) e XOR fixo da norma. */
    fun formatBits(mask: Int): Int {
        val data = (0b00 shl 3) or mask // 00 = nível M
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
            // MSB primeiro: o bit 14 vai para (8,0). Escrever LSB primeiro inverte a palavra
            // inteira — o leitor ainda "conserta" via BCH e chega a uma máscara plausível,
            // mas errada, e os dados saem desmascarados de forma incorreta.
            val bit = (format shr (14 - i)) and 1 == 1
            // Cópia junto ao localizador superior esquerdo.
            when {
                i < 6 -> m[8][i] = bit
                i == 6 -> m[8][7] = bit
                i == 7 -> m[8][8] = bit
                i == 8 -> m[7][8] = bit
                else -> m[14 - i][8] = bit
            }
            // Segunda cópia: 7 módulos subindo pela coluna 8 (bits 0–6) e 8 módulos na linha 8
            // (bits 7–14). A divisão é 7+8, não 8+7 — com 8+7 o bit 7 cairia justamente sobre
            // o módulo escuro fixo e se perderia.
            if (i < 7) m[size - 1 - i][8] = bit else m[8][size - 8 + (i - 7)] = bit
        }
        m[size - 8][8] = true
    }

    // ---------------------------------------------------------- penalidade

    private fun penalty(m: Array<Array<Boolean?>>): Int {
        val size = m.size
        val g = Array(size) { r -> BooleanArray(size) { c -> m[r][c] == true } }
        var score = 0

        // Regra 1: sequências de 5+ módulos iguais.
        for (i in 0 until size) {
            score += runPenalty(BooleanArray(size) { g[i][it] })
            score += runPenalty(BooleanArray(size) { g[it][i] })
        }
        // Regra 2: blocos 2×2 de mesma cor.
        for (r in 0 until size - 1) for (c in 0 until size - 1) {
            if (g[r][c] == g[r][c + 1] && g[r][c] == g[r + 1][c] && g[r][c] == g[r + 1][c + 1]) {
                score += 3
            }
        }
        // Regra 3: padrão 1:1:3:1:1 que imita um localizador.
        val pattern = booleanArrayOf(true, false, true, true, true, false, true, false, false, false, false)
        for (r in 0 until size) for (c in 0 until size - 10) {
            if ((0..10).all { g[r][c + it] == pattern[it] }) score += 40
            if ((0..10).all { g[r][c + it] == pattern[10 - it] }) score += 40
        }
        for (c in 0 until size) for (r in 0 until size - 10) {
            if ((0..10).all { g[r + it][c] == pattern[it] }) score += 40
            if ((0..10).all { g[r + it][c] == pattern[10 - it] }) score += 40
        }
        // Regra 4: desvio da proporção de 50% de módulos escuros.
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

/** Buffer de bits MSB-first. */
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

/** Reed–Solomon sobre GF(256) com o polinômio primitivo 0x11D, como usado pelo QR. */
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

    /** Polinômio gerador de grau [degree]: ∏ (x − α^i). */
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

    /** Devolve os [ecCount] codewords de correção do bloco [data]. */
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

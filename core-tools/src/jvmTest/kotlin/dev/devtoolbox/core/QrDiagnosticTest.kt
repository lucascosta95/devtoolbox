package dev.devtoolbox.core

import com.google.zxing.common.reedsolomon.GenericGF
import com.google.zxing.common.reedsolomon.ReedSolomonEncoder
import dev.devtoolbox.core.util.ReedSolomon
import kotlin.test.Test
import kotlin.test.assertEquals

/** Isola em qual camada o encoder diverge da ZXing. */
class QrDiagnosticTest {

    @Test
    fun reedSolomonMatchesZxing() {
        val data = IntArray(28) { (it * 37 + 11) and 0xFF }
        val ecCount = 16

        val ours = ReedSolomon.encode(data, ecCount).toList()

        val buffer = data.copyOf(data.size + ecCount)
        ReedSolomonEncoder(GenericGF.QR_CODE_FIELD_256).encode(buffer, ecCount)
        val theirs = buffer.drop(data.size)

        assertEquals(theirs, ours, "codewords de correção divergem")
    }

    @Test
    fun generatorPolynomialMatchesZxing() {
        // g10, publicado na norma: 1, 216, 194, 159, 111, 199, 94, 95, 113, 157, 193.
        assertEquals(
            listOf(1, 216, 194, 159, 111, 199, 94, 95, 113, 157, 193),
            ReedSolomon.generator(10).toList(),
        )
    }
}

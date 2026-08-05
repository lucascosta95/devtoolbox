package dev.devtoolbox.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.Decoder
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dev.devtoolbox.core.util.QrCode
import kotlin.test.Test
import kotlin.test.assertEquals

class QrAgainstZxingTest {

    private val samples = listOf(
        "https://devtoolbox.dev",
        "OI",
        "a",
        "teste 123",
        "https://exemplo.com/caminho?com=parametros&e=coisas",
        "a".repeat(40),
    )

    @Test
    fun zxingDecodesOurMatrix() {
        for (text in samples) {
            val decoded = Decoder().decode(toBitMatrix(QrCode.encode(text))).text
            assertEquals(text, decoded, "a ZXing não leu de volta: \"$text\"")
        }
    }

    @Test
    fun zxingAlsoDecodesTheSameContentItEncodes() {
        for (text in samples) {
            assertEquals(text, Decoder().decode(toBitMatrix(encodeWithZxing(text))).text)
        }
    }

    private fun toBitMatrix(modules: List<List<Boolean>>): BitMatrix {
        val matrix = BitMatrix(modules.size, modules.size)
        for (r in modules.indices) {
            for (c in modules[r].indices) {
                if (modules[r][c]) matrix.set(c, r)
            }
        }
        return matrix
    }

    private fun encodeWithZxing(text: String): List<List<Boolean>> {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        return (0 until matrix.height).map { r -> (0 until matrix.width).map { c -> matrix.get(c, r) } }
    }
}

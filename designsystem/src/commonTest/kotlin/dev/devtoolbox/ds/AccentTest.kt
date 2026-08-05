package dev.devtoolbox.ds

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccentTest {

    private fun assertClose(expected: Color, actual: Color, tolerance: Float = 0.02f) {
        val delta = maxOf(
            abs(expected.red - actual.red),
            abs(expected.green - actual.green),
            abs(expected.blue - actual.blue),
        )
        assertTrue(delta <= tolerance, "esperado $expected, recebido $actual (Δ=$delta)")
    }

    @Test
    fun mixingWithRatioOneOrZeroReturnsTheEndpoints() {
        val a = Color(0xFF9184D9)
        val b = Color(0xFF292B31)
        assertClose(a, Oklab.mix(a, b, 1f))
        assertClose(b, Oklab.mix(a, b, 0f))
    }

    @Test
    fun mixingIsSymmetricWhenTheRatioIsMirrored() {
        val a = Color(0xFF5AA9C9)
        val b = Color(0xFFF5F4FF)
        assertClose(Oklab.mix(a, b, 0.3f), Oklab.mix(b, a, 0.7f))
    }

    @Test
    fun mixInterpolatesPerceptuallyNotInLinearLight() {
        val mixed = Oklab.mix(Color(0xFF9184D9), Color(0xFF292B31), 0.5f)
        val linearLightMid = ((0x91 / 255f).srgbToLinear() + (0x29 / 255f).srgbToLinear()) / 2f

        assertTrue(
            mixed.red < linearLightMid.linearToSrgb(),
            "mistura em luz linear deveria ser mais clara que a mistura em OKLab",
        )
        assertClose(Color(90 / 255f, 86 / 255f, 128 / 255f), mixed, tolerance = 0.01f)
    }

    private fun Float.srgbToLinear(): Float =
        if (this <= 0.04045f) this / 12.92f else ((this + 0.055f) / 1.055f).pow(2.4f)

    private fun Float.linearToSrgb(): Float =
        if (this <= 0.0031308f) this * 12.92f else 1.055f * this.pow(1f / 2.4f) - 0.055f

    @Test
    fun darkTokensAreLighterThanTheAccentAndTheInkIsDarker() {
        val tokens = AccentTokens.derive(AccentColor.Blurple.color, isDark = true)
        val accentLuma = luma(tokens.accent)

        assertTrue(luma(tokens.accent300) > accentLuma)
        assertTrue(luma(tokens.accent400) > accentLuma)
        assertTrue(luma(tokens.accent900) < accentLuma)
        assertTrue(luma(tokens.accent300) > luma(tokens.accent400))
    }

    @Test
    fun lightTokensDarkenTheTextAndLightenTheInk() {
        val tokens = AccentTokens.derive(AccentColor.Blurple.color, isDark = false)
        val accentLuma = luma(tokens.accent)

        assertTrue(luma(tokens.accent300) < accentLuma, "texto sobre tinta clara precisa escurecer")
        assertTrue(luma(tokens.accent400) < accentLuma)
        assertTrue(luma(tokens.accent900) > accentLuma, "a tinta no tema claro é quase branca")
    }

    @Test
    fun everyPaletteKeepsTheChosenColorAsTheAccentToken() {
        for (accent in AccentColor.entries) {
            for (dark in listOf(true, false)) {
                assertEquals(accent.color, AccentTokens.derive(accent.color, dark).accent)
            }
        }
    }

    @Test
    fun themeColorsFollowTheChosenAccent() {
        val teal = colorsFor(ThemeMode.Dark, AccentColor.Teal)
        assertEquals(AccentColor.Teal.color, teal.accent)
        assertEquals(teal.accents.accent900, teal.accentSurface)
        assertEquals(teal.accents.accent300, teal.onAccentSurface)
        assertTrue(teal.accentSurface != colorsFor(ThemeMode.Dark, AccentColor.Rose).accentSurface)
    }

    @Test
    fun idsRoundTripAndUnknownOnesFallBack() {
        for (accent in AccentColor.entries) {
            assertEquals(accent, AccentColor.byId(accent.id))
        }
        assertNull(AccentColor.byId("cor-que-nao-existe"))
        assertNull(AccentColor.byId(null))
        assertEquals(AccentColor.Blurple, AccentColor.default)
    }

    @Test
    fun theSixPaletteColorsMatchTheHandoff() {
        assertEquals(
            listOf("#9184D9", "#5AA9C9", "#5CB5A3", "#8FB95F", "#D9A544", "#D9788F"),
            AccentColor.entries.map { hex(it.color) },
        )
    }

    private fun luma(color: Color): Float =
        0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

    private fun hex(color: Color): String {
        fun channel(value: Float) = (value * 255f + 0.5f).toInt()
            .toString(16).uppercase().padStart(2, '0')
        return "#${channel(color.red)}${channel(color.green)}${channel(color.blue)}"
    }
}

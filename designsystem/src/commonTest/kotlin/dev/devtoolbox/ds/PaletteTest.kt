package dev.devtoolbox.ds

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaletteTest {

    @Test
    fun paletteIdsAreUnique() {
        val ids = Palettes.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ThemeChoice.Custom.id !in ids)
    }

    @Test
    fun theLegacyDarkAndLightIdsStillPointToNocturne() {
        assertEquals(ThemeChoice.Preset(Palettes.NocturneDark), ThemeChoice.byId("dark"))
        assertEquals(ThemeChoice.Preset(Palettes.NocturneLight), ThemeChoice.byId("light"))
        assertEquals(ThemeChoice.Custom, ThemeChoice.byId("custom"))
        assertNull(ThemeChoice.byId("tema-removido"))
    }

    @Test
    fun everyPaletteHasReadableText() {
        for (palette in Palettes.all) {
            assertTrue(
                contrast(palette.text, palette.bg) >= 7.0,
                "${palette.label}: contraste ${contrast(palette.text, palette.bg)}",
            )
            assertTrue(
                contrast(palette.text, palette.surface) >= 7.0,
                "${palette.label} (superfície): contraste ${contrast(palette.text, palette.surface)}",
            )
        }
    }

    @Test
    fun theDefaultCustomThemeLooksLikeNocturneDark() {
        val palette = CustomTheme().palette()

        assertEquals(Palettes.NocturneDark.bg.toHex(), palette.bg.toHex())
        assertEquals(Palettes.NocturneDark.surface.toHex(), palette.surface.toHex())
        assertEquals(AccentColor.default.color.toHex(), CustomTheme().accentColor().toHex())
        assertTrue(palette.isDark)
    }

    @Test
    fun aLightCustomBackgroundGetsDarkText() {
        val palette = CustomTheme(bg = "#f4f2ec", surface = "#e8e3d8", accent = "#c2492f").palette()

        assertFalse(palette.isDark)
        assertTrue(contrast(palette.text, palette.bg) >= 7.0)
    }

    @Test
    fun customPresetsAreReadable() {
        for (preset in customThemePresets) {
            val palette = preset.theme.palette()
            assertTrue(contrast(palette.text, palette.bg) >= 7.0, preset.label)
        }
    }

    @Test
    fun anInvalidHexFallsBackInsteadOfBreaking() {
        val theme = CustomTheme(bg = "#12", surface = "zzzzzz", accent = "")

        assertEquals(Ramp.bgDark, theme.palette().bg)
        assertEquals(Ramp.surfaceDark, theme.palette().surface)
        assertEquals(AccentColor.default.color, theme.accentColor())
    }

    @Test
    fun hexParsingAcceptsTheUsualSpellings() {
        assertEquals(Color(0xFF9184D9), parseHexColor("#9184d9"))
        assertEquals(Color(0xFF9184D9), parseHexColor("9184D9"))
        assertEquals(Color(0xFF9184D9), parseHexColor("  #9184D9 "))
        assertNull(parseHexColor("#9184d"))
        assertEquals("#9184d9", Color(0xFF9184D9).toHex())
    }

    private fun contrast(a: Color, b: Color): Double {
        fun channel(c: Float) = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        fun lum(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
        val (hi, lo) = listOf(lum(a), lum(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }
}

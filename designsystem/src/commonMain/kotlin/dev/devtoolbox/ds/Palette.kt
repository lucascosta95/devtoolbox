package dev.devtoolbox.ds

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

@Immutable
data class Palette(
    val id: String,
    val label: String,
    val bg: Color,
    val surface: Color,
    val text: Color,
) {
    val isDark: Boolean get() = isDarkBackground(bg)
}

object Palettes {
    val NocturneDark = Palette("dark", "Nocturne", Ramp.bgDark, Ramp.surfaceDark, Ramp.textDark)
    val Graphite = Palette("graphite", "Grafite", Color(0xFF18181B), Color(0xFF242427), Color(0xFFE6E6E8))
    val Oled = Palette("oled", "Preto OLED", Color(0xFF000000), Color(0xFF131315), Color(0xFFE8E8EA))
    val Ocean = Palette("ocean", "Oceano", Color(0xFF0F1A24), Color(0xFF182734), Color(0xFFE2EAF0))
    val Forest = Palette("forest", "Floresta", Color(0xFF111815), Color(0xFF1B2520), Color(0xFFE3EAE5))
    val Mocha = Palette("mocha", "Café", Color(0xFF1A1614), Color(0xFF27211E), Color(0xFFECE4DE))
    val NocturneLight = Palette("light", "Nocturne claro", Ramp.neutral100, Ramp.neutral200, Ramp.neutral900)
    val Mist = Palette("mist", "Névoa", Color(0xFFF5F5F6), Color(0xFFE6E6EA), Color(0xFF26262B))
    val Paper = Palette("paper", "Papel", Color(0xFFF6F3EC), Color(0xFFEAE4D8), Color(0xFF2B2722))

    val all = listOf(NocturneDark, Graphite, Oled, Ocean, Forest, Mocha, NocturneLight, Mist, Paper)

    val default = NocturneDark

    fun byId(id: String?): Palette? = all.firstOrNull { it.id == id }
}

sealed interface ThemeChoice {
    val id: String

    data class Preset(val palette: Palette) : ThemeChoice {
        override val id: String get() = palette.id
    }

    data object Custom : ThemeChoice {
        override val id = "custom"
    }

    companion object {
        val default: ThemeChoice = Preset(Palettes.default)

        fun byId(id: String?): ThemeChoice? =
            if (id == Custom.id) Custom else Palettes.byId(id)?.let(::Preset)
    }
}

@Immutable
data class CustomTheme(
    val bg: String = Ramp.bgDark.toHex(),
    val surface: String = Ramp.surfaceDark.toHex(),
    val accent: String = AccentColor.default.color.toHex(),
) {
    fun palette(): Palette {
        val bg = parseHexColor(bg) ?: Ramp.bgDark
        val surface = parseHexColor(surface) ?: Ramp.surfaceDark
        val ink = if (isDarkBackground(bg)) Color.White else Color.Black
        return Palette(
            id = ThemeChoice.Custom.id,
            label = "Personalizado",
            bg = bg,
            surface = surface,
            text = Oklab.mix(ink, bg, if (isDarkBackground(bg)) 0.90f else 0.86f),
        )
    }

    fun accentColor(): Color = parseHexColor(accent) ?: AccentColor.default.color
}

data class CustomThemePreset(val label: String, val theme: CustomTheme)

val customThemePresets = listOf(
    CustomThemePreset("Tomate", CustomTheme("#141315", "#1f1d20", "#e2513c")),
    CustomThemePreset("Índigo", CustomTheme("#14161f", "#1e2130", "#7d8cff")),
    CustomThemePreset("Esmeralda", CustomTheme("#111815", "#1b2420", "#3fbd85")),
    CustomThemePreset("Âmbar", CustomTheme("#181510", "#241f18", "#f0a02e")),
    CustomThemePreset("Ametista", CustomTheme("#161320", "#211d2e", "#a97bf0")),
    CustomThemePreset("Papel", CustomTheme("#f4f2ec", "#e8e3d8", "#c2492f")),
)

fun parseHexColor(hex: String): Color? {
    val digits = hex.trim().removePrefix("#")
    if (digits.length != 6) return null
    val value = digits.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or value)
}

fun Color.toHex(): String {
    fun channel(value: Float) = (value * 255f).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0')
    return "#${channel(red)}${channel(green)}${channel(blue)}"
}

fun isDarkBackground(bg: Color): Boolean =
    (0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue) * 255.0 < 140.0

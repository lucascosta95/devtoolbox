package dev.devtoolbox.ds

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Ramp {
    val neutral100 = Color(0xFFF3F5FE)
    val neutral200 = Color(0xFFE4E7F5)
    val neutral300 = Color(0xFFCFD3E5)
    val neutral400 = Color(0xFFB2B6CA)
    val neutral500 = Color(0xFF9397AB)
    val neutral600 = Color(0xFF75798C)
    val neutral700 = Color(0xFF595D6C)
    val neutral800 = Color(0xFF3F424D)
    val neutral900 = Color(0xFF292B31)

    val accent100 = Color(0xFFF5F4FF)
    val accent200 = Color(0xFFE7E5FE)
    val accent300 = Color(0xFFD2CEFD)
    val accent400 = Color(0xFFB5ABFC)
    val accent500 = Color(0xFF968AE0)
    val accent600 = Color(0xFF796CBF)
    val accent700 = Color(0xFF5D5294)
    val accent800 = Color(0xFF423A6A)
    val accent900 = Color(0xFF2B2741)

    val bgDark = Color(0xFF161826)
    val surfaceDark = Color(0xFF232532)
    val textDark = Color(0xFFE9E9ED)
    val accent = Color(0xFF9184D9)
}

@Immutable
data class NocturneColors(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val divider: Color,
    val isDark: Boolean,
    val accents: AccentTokens,
) {
    val accent: Color get() = accents.accent

    fun text(alpha: Float): Color = text.copy(alpha = alpha)

    val hoverTint: Color get() = text.copy(alpha = 0.07f)

    val accentSurface: Color get() = accents.accent900

    val onAccentSurface: Color get() = accents.accent300

    val accentPressed: Color get() = accents.accent400

    val mutedSurface: Color get() = text.copy(alpha = 0.08f)
}

fun colorsFor(mode: ThemeMode, accent: AccentColor): NocturneColors {
    val isDark = mode == ThemeMode.Dark
    val accents = AccentTokens.derive(accent.color, isDark)
    return if (isDark) {
        NocturneColors(
            bg = Ramp.bgDark,
            surface = Ramp.surfaceDark,
            text = Ramp.textDark,
            divider = Ramp.textDark.copy(alpha = 0.16f),
            isDark = true,
            accents = accents,
        )
    } else {
        NocturneColors(
            bg = Ramp.neutral100,
            surface = Ramp.neutral200,
            text = Ramp.neutral900,
            divider = Ramp.neutral900.copy(alpha = 0.16f),
            isDark = false,
            accents = accents,
        )
    }
}

val DarkColors = colorsFor(ThemeMode.Dark, AccentColor.default)

val LightColors = colorsFor(ThemeMode.Light, AccentColor.default)

@Immutable
data class Spacing(
    val xxs: Dp = 2.8.dp,
    val xs: Dp = 5.6.dp,
    val sm: Dp = 8.4.dp,
    val md: Dp = 11.2.dp,
    val lg: Dp = 16.8.dp,
    val xl: Dp = 22.4.dp,
)

@Immutable
data class Radii(
    val sm: Dp = 4.dp,
    val md: Dp = 8.dp,
    val lg: Dp = 14.dp,
)

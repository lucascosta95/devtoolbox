package dev.devtoolbox.ds

import androidx.compose.ui.graphics.Color

/**
 * Cores de destaque oferecidas na barra de título.
 *
 * O [id] é o que vai para disco — os hexadecimais podem ser reajustados sem invalidar a
 * preferência já salva de quem instalou uma versão anterior.
 */
enum class AccentColor(val id: String, val label: String, val color: Color) {
    Blurple("blurple", "Blurple", Color(0xFF9184D9)),
    Cyan("cyan", "Ciano", Color(0xFF5AA9C9)),
    Teal("teal", "Teal", Color(0xFF5CB5A3)),
    Lime("lime", "Lima", Color(0xFF8FB95F)),
    Amber("amber", "Âmbar", Color(0xFFD9A544)),
    Rose("rose", "Rosa", Color(0xFFD9788F));

    companion object {
        val default = Blurple

        fun byId(id: String?): AccentColor? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Tokens de accent derivados da cor escolhida, por mistura em OKLab.
 *
 * As proporções vêm do handoff e são diferentes por tema: no escuro a cor é clareada contra
 * `accent-100` para ficar legível sobre a tinta; no claro ela é escurecida contra `neutral-900`.
 */
data class AccentTokens(
    val accent: Color,
    /** Texto e ícone sobre a tinta de fundo. */
    val accent300: Color,
    /** Estado pressionado. */
    val accent400: Color,
    /** Tinta de fundo (item selecionado, badge, destaque do diff). */
    val accent900: Color,
) {
    companion object {
        private val Light = Ramp.accent100 // #F5F4FF
        private val Dark = Ramp.neutral900 // #292B31

        fun derive(accent: Color, isDark: Boolean): AccentTokens = if (isDark) {
            AccentTokens(
                accent = accent,
                accent300 = Oklab.mix(accent, Light, 0.45f),
                accent400 = Oklab.mix(accent, Light, 0.72f),
                accent900 = Oklab.mix(accent, Dark, 0.22f),
            )
        } else {
            AccentTokens(
                accent = accent,
                accent300 = Oklab.mix(accent, Dark, 0.74f),
                accent400 = Oklab.mix(accent, Dark, 0.88f),
                accent900 = Oklab.mix(accent, Light, 0.15f),
            )
        }
    }
}

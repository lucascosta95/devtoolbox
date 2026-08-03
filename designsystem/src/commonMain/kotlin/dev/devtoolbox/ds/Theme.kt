package dev.devtoolbox.ds

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import dev.devtoolbox.ds.resources.Res
import dev.devtoolbox.ds.resources.inter_medium
import dev.devtoolbox.ds.resources.inter_regular
import dev.devtoolbox.ds.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

enum class ThemeMode { Dark, Light;

    fun toggled() = if (this == Dark) Light else Dark
}

/**
 * Escala tipográfica da tela, em sp, exatamente como no handoff.
 * Peso de título é 500 — nunca mais forte.
 */
@Immutable
data class NocturneTypography(
    val toolTitle: TextStyle,
    val validatedValue: TextStyle,
    val body: TextStyle,
    val item: TextStyle,
    val mono: TextStyle,
    val label: TextStyle,
    val tag: TextStyle,
    val sectionHeader: TextStyle,
    val kicker: TextStyle,
)

val LocalColors: ProvidableCompositionLocal<NocturneColors> = staticCompositionLocalOf { DarkColors }
val LocalTypography = staticCompositionLocalOf<NocturneTypography> {
    error("NocturneTypography não fornecido — envolva a UI em NocturneTheme")
}
val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalRadii = staticCompositionLocalOf { Radii() }

object Nocturne {
    val colors: NocturneColors @Composable get() = LocalColors.current
    val type: NocturneTypography @Composable get() = LocalTypography.current
    val space: Spacing @Composable get() = LocalSpacing.current
    val radii: Radii @Composable get() = LocalRadii.current
}

@Composable
private fun interFamily() = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
)

@Composable
private fun monoFamily() = FontFamily(Font(Res.font.jetbrains_mono_regular, FontWeight.Normal))

@Composable
private fun typography(): NocturneTypography {
    val inter = interFamily()
    val mono = monoFamily()
    val base = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal)
    return NocturneTypography(
        toolTitle = base.copy(fontSize = 21.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
        validatedValue = base.copy(fontFamily = mono, fontSize = 15.sp, lineHeight = 22.sp),
        body = base.copy(fontSize = 14.sp, lineHeight = 21.sp),
        item = base.copy(fontSize = 13.sp, lineHeight = 18.sp),
        mono = base.copy(fontFamily = mono, fontSize = 12.5.sp, lineHeight = 20.sp),
        label = base.copy(fontSize = 12.sp, lineHeight = 17.sp),
        tag = base.copy(fontSize = 11.sp, lineHeight = 15.sp),
        sectionHeader = base.copy(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.63.sp, // 0.06em
            lineHeight = 14.sp,
        ),
        kicker = base.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp, // 0.1em
            lineHeight = 14.sp,
        ),
    )
}

/**
 * Tema próprio do DevToolbox — sem Material.
 *
 * Material traria uma paleta e componentes que teríamos de sobrescrever inteiros; o Nocturne
 * é pequeno o bastante para viver em `foundation` + estes CompositionLocals.
 */
@Composable
fun NocturneTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val colors = if (mode == ThemeMode.Dark) DarkColors else LightColors
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides typography(),
        LocalSpacing provides Spacing(),
        LocalRadii provides Radii(),
        // O DS não usa ripple: hover e pressed são tintas do ramp.
        LocalIndication provides NoIndication,
    ) {
        Box(Modifier.fillMaxSize().background(colors.bg)) { content() }
    }
}

/** Sublinhado/strikethrough usados pelo diff — exposto para não repetir import. */
val LineThrough: TextDecoration get() = TextDecoration.LineThrough

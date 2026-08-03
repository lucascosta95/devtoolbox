package dev.devtoolbox.ds.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.resources.Res
import dev.devtoolbox.ds.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Ícones Phosphor (peso *regular*; *fill* só em estrela favoritada e badges de status).
 *
 * O mapa nome→recurso é a única ponte entre o `icon: String` do core e os SVGs — um teste
 * do app garante que todo ícone referenciado pelo [dev.devtoolbox.core.ToolRegistry] existe aqui.
 */
object PhosphorIcons {

    private val regular: Map<String, DrawableResource> = mapOf(
        "wrench" to Res.drawable.ph_wrench,
        "magnifying-glass" to Res.drawable.ph_magnifying_glass,
        "star" to Res.drawable.ph_star,
        "clock-counter-clockwise" to Res.drawable.ph_clock_counter_clockwise,
        "sun" to Res.drawable.ph_sun,
        "moon" to Res.drawable.ph_moon,
        "arrow-right" to Res.drawable.ph_arrow_right,
        "check" to Res.drawable.ph_check,
        "copy" to Res.drawable.ph_copy,
        "caret-down" to Res.drawable.ph_caret_down,
        "lock-key" to Res.drawable.ph_lock_key,
        "code" to Res.drawable.ph_code,
        "text-aa" to Res.drawable.ph_text_aa,
        "magic-wand" to Res.drawable.ph_magic_wand,
        "check-circle" to Res.drawable.ph_check_circle,
        "x-circle" to Res.drawable.ph_x_circle,
        "arrows-left-right" to Res.drawable.ph_arrows_left_right,
        "link-simple" to Res.drawable.ph_link_simple,
        "brackets-curly" to Res.drawable.ph_brackets_curly,
        "file-text" to Res.drawable.ph_file_text,
        "terminal-window" to Res.drawable.ph_terminal_window,
        "arrows-split" to Res.drawable.ph_arrows_split,
        "paragraph" to Res.drawable.ph_paragraph,
        "identification-badge" to Res.drawable.ph_identification_badge,
        "palette" to Res.drawable.ph_palette,
        "clock-countdown" to Res.drawable.ph_clock_countdown,
        "calendar-check" to Res.drawable.ph_calendar_check,
        "qr-code" to Res.drawable.ph_qr_code,
        "identification-card" to Res.drawable.ph_identification_card,
        "buildings" to Res.drawable.ph_buildings,
        "phone" to Res.drawable.ph_phone,
        "key" to Res.drawable.ph_key,
        "fingerprint" to Res.drawable.ph_fingerprint,
        "credit-card" to Res.drawable.ph_credit_card,
        "drop-half" to Res.drawable.ph_drop_half,
        "image" to Res.drawable.ph_image,
        "image-square" to Res.drawable.ph_image_square,
    )

    private val filled: Map<String, DrawableResource> = mapOf(
        "star" to Res.drawable.ph_star_fill,
        "check-circle" to Res.drawable.ph_check_circle_fill,
        "x-circle" to Res.drawable.ph_x_circle_fill,
    )

    fun resource(name: String, fill: Boolean = false): DrawableResource? =
        if (fill) filled[name] ?: regular[name] else regular[name]

    /** Nomes disponíveis — usado pelo teste de cobertura de ícones. */
    val names: Set<String> get() = regular.keys
}

@Composable
fun PhosphorIcon(
    name: String,
    size: Dp = 16.dp,
    tint: Color = Nocturne.colors.text,
    fill: Boolean = false,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    val res = PhosphorIcons.resource(name, fill) ?: return
    Image(
        painter = painterResource(res),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
    )
}

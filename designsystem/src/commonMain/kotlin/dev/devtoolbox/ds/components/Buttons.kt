package dev.devtoolbox.ds.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.devtoolbox.ds.DISABLED_ALPHA
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.focusRing

/**
 * Botão *outlined* — o DS nunca preenche botões: borda 1 px accent sobre transparente.
 * Hover/pressed vêm do ramp, foco é o anel accent.
 */
@Composable
fun OutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    iconFill: Boolean = false,
    enabled: Boolean = true,
    accentBorder: Boolean = true,
    /** `false` = variante fantasma: sem borda, só hover. */
    bordered: Boolean = true,
    contentColor: Color = Nocturne.colors.text,
) {
    val colors = Nocturne.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }

    val fill = when {
        pressed -> colors.accent.copy(alpha = 0.18f)
        hovered -> colors.hoverTint
        else -> Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .onFocusChanged { focused = it.isFocused }
            .focusRing(focused, Nocturne.radii.md)
            .clip(RoundedCornerShape(Nocturne.radii.md))
            .background(fill)
            .then(
                if (bordered) {
                    Modifier.border(
                        width = 1.dp,
                        color = if (accentBorder) colors.accent else colors.divider,
                        shape = RoundedCornerShape(Nocturne.radii.md),
                    )
                } else {
                    Modifier
                },
            )
            .hoverable(interaction, enabled)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = Nocturne.space.md, vertical = Nocturne.space.xs),
    ) {
        if (icon != null) PhosphorIcon(icon, size = 14.dp, tint = contentColor, fill = iconFill)
        Text(label, style = Nocturne.type.mono, color = contentColor)
    }
}

/** Variante `.btn-secondary`: borda neutra, para ações de apoio ("Gerar novo exemplo"). */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    enabled: Boolean = true,
) = OutlinedButton(
    label = label,
    onClick = onClick,
    modifier = modifier,
    icon = icon,
    enabled = enabled,
    accentBorder = false,
    contentColor = Nocturne.colors.text(0.8f),
)

/** Botão-ícone quadrado, sem borda — title bar, favoritar, fechar. */
@Composable
fun IconButton(
    icon: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    boxSize: Dp = 30.dp,
    iconSize: Dp = 15.dp,
    tint: Color = Nocturne.colors.text(0.65f),
    fill: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(boxSize)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .onFocusChanged { focused = it.isFocused }
            .focusRing(focused, Nocturne.radii.sm)
            .clip(RoundedCornerShape(Nocturne.radii.sm))
            .background(if (hovered) Nocturne.colors.hoverTint else Color.Transparent)
            .hoverable(interaction, enabled)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
    ) {
        PhosphorIcon(icon, size = iconSize, tint = tint, fill = fill)
    }
}

internal val NoPadding = PaddingValues(0.dp)

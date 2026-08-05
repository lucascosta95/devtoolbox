package dev.devtoolbox.ds.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.AccentColor
import dev.devtoolbox.ds.Nocturne

private val SWATCH_SIZE = 13.dp
private val SWATCH_GAP = 7.dp

@Composable
fun AccentPicker(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SWATCH_GAP),
    ) {
        PhosphorIcon("drop-half", size = 14.dp, tint = Nocturne.colors.text(0.45f))
        for (accent in AccentColor.entries) {
            Swatch(accent, selected = accent == selected, onSelect = { onSelect(accent) })
        }
    }
}

@Composable
private fun Swatch(accent: AccentColor, selected: Boolean, onSelect: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val ringInset = 3.5.dp

    Box(
        modifier = Modifier
            .size(SWATCH_SIZE + ringInset * 2)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .semantics {
                this.selected = selected
                contentDescription = "Cor de destaque ${accent.label}"
            },
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (selected) SWATCH_SIZE + ringInset * 2 else SWATCH_SIZE)
                .alpha(if (selected) 1f else 0.7f)
                .then(
                    if (selected) {
                        Modifier
                            .border(1.5.dp, accent.color, CircleShape)
                            .padding(1.5.dp)
                            .border(2.dp, Nocturne.colors.surface, CircleShape)
                            .padding(2.dp)
                    } else {
                        Modifier
                    },
                )
                .clip(CircleShape)
                .background(accent.color),
        )
    }
}

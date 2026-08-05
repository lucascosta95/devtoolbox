package dev.devtoolbox.ds.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.focusRing

@Composable
fun SidebarItem(
    label: String,
    icon: String,
    selected: Boolean,
    favorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Nocturne.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }

    val background = when {
        selected -> colors.accentSurface
        hovered -> colors.hoverTint
        else -> Color.Transparent
    }
    val foreground = if (selected) colors.onAccentSurface else colors.text

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusRing(focused, Nocturne.radii.sm)
            .clip(RoundedCornerShape(Nocturne.radii.sm))
            .background(background)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .semantics {
                this.selected = selected
                stateDescription = if (favorite) "favoritada" else "não favoritada"
            }
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        PhosphorIcon(icon, size = 15.dp, tint = foreground)
        Text(
            label,
            style = Nocturne.type.item,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            icon = "star",
            fill = favorite,
            contentDescription = if (favorite) "Remover $label dos favoritos" else "Favoritar $label",
            onClick = onToggleFavorite,
            boxSize = 18.dp,
            iconSize = 13.dp,
            tint = if (favorite) colors.accent else foreground.copy(alpha = 0.35f),
        )
    }
}

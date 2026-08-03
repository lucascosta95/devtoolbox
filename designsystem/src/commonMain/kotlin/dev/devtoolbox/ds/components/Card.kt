package dev.devtoolbox.ds.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.Nocturne

/** Card `.card` do DS: fundo surface, borda divider, raio md. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Nocturne.radii.md))
            .background(Nocturne.colors.surface)
            .border(1.dp, Nocturne.colors.divider, RoundedCornerShape(Nocturne.radii.md))
            .padding(Nocturne.space.md),
        content = content,
    )
}

/** Kicker do card: 10 sp, uppercase, tracking 0.1em, accent. */
@Composable
fun CardKicker(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text.uppercase(),
            style = Nocturne.type.kicker,
            color = Nocturne.colors.onAccentSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** Cabeçalho de seção da sidebar: 10.5 sp, uppercase, tracking 0.06em, 50% + ícone. */
@Composable
fun SectionHeader(text: String, icon: String, iconFill: Boolean = false, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(
            start = Nocturne.space.xs,
            end = Nocturne.space.xs,
            top = Nocturne.space.md,
            bottom = Nocturne.space.xxs,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
    ) {
        PhosphorIcon(
            icon,
            size = 11.dp,
            fill = iconFill,
            tint = if (iconFill) Nocturne.colors.accent else Nocturne.colors.text(0.5f),
        )
        Text(
            text.uppercase(),
            style = Nocturne.type.sectionHeader,
            color = Nocturne.colors.text(0.5f),
        )
    }
}

/** Tag `.tag-neutral` / `.tag-accent`: 11 sp, padding 3×10, raio 6. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    val colors = Nocturne.colors
    Text(
        text,
        style = Nocturne.type.tag,
        color = if (accent) colors.onAccentSurface else colors.text(0.6f),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (accent) colors.accentSurface else colors.mutedSurface)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

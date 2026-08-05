package dev.devtoolbox.ds.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.Nocturne

private val LABEL_WIDTH = 170.dp

@Composable
fun LabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelWidth: Dp = LABEL_WIDTH,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Nocturne.space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
    ) {
        Text(
            label,
            style = Nocturne.type.label,
            color = Nocturne.colors.text(0.55f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(labelWidth),
        )
        Text(
            value,
            style = Nocturne.type.item,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Nocturne.colors.divider))
}

@Composable
fun StatusBadge(valid: Boolean, modifier: Modifier = Modifier) {
    val colors = Nocturne.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (valid) colors.accentSurface else colors.mutedSurface)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        PhosphorIcon(
            if (valid) "check-circle" else "x-circle",
            size = 14.dp,
            fill = true,
            tint = if (valid) colors.onAccentSurface else colors.text(0.6f),
        )
        Text(
            if (valid) "Válido" else "Inválido",
            style = Nocturne.type.tag,
            color = if (valid) colors.onAccentSurface else colors.text(0.6f),
        )
    }
}

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
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.Nocturne

/** Coluna de labels — 170 dp acomoda "Dígito verificador" em uma linha na JetBrains Mono. */
private val LABEL_WIDTH = 170.dp

/** Linha label/valor: label 12 sp a 55%, valor 13 sp. */
@Composable
fun LabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
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
            // JetBrains Mono é mais larga que a Inter: 150 dp já não cabia os labels longos.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(LABEL_WIDTH),
        )
        Text(
            value,
            style = Nocturne.type.item,
            // Hashes e tokens quebram em várias linhas; passando de três, elipse — o valor
            // inteiro continua a um clique de distância, no botão de copiar.
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** Divisor de 1 px na cor divider. */
@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Nocturne.colors.divider))
}

/** Badge de status dos validadores: accent quando válido, neutro quando inválido. */
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

package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.Divider
import dev.devtoolbox.ds.components.LabelValueRow
import dev.devtoolbox.ds.components.SecondaryButton

/**
 * Arquétipos `list` e `listgen`: linhas label/valor separadas por divisor.
 *
 * Com [ToolBody.Rows.regenerable] aparece "Gerar novo exemplo" e cada linha ganha um botão
 * de copiar — é o que separa `listgen` de `list` no handoff.
 */
@Composable
fun RowsLayout(
    body: ToolBody.Rows,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        if (body.regenerable) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Nocturne.space.sm),
                horizontalArrangement = Arrangement.End,
            ) {
                SecondaryButton("Gerar novo exemplo", onClick = onRegenerate)
            }
        }

        Card {
            for ((index, row) in body.rows.withIndex()) {
                row.swatch?.let { Swatch(it) }
                LabelValueRow(
                    label = row.label,
                    value = row.value,
                    trailing = if (body.regenerable) {
                        { CopyButton(row.value, label = "") }
                    } else {
                        null
                    },
                )
                if (index != body.rows.lastIndex) Divider()
            }
        }
    }
}

/** Amostra de cor do Color Converter: 56 dp de altura, raio sm, borda divider. */
@Composable
private fun Swatch(hex: String) {
    val color = parseHex(hex) ?: return
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(bottom = Nocturne.space.xs)
            .clip(RoundedCornerShape(Nocturne.radii.sm))
            .background(color)
            .border(1.dp, Nocturne.colors.divider, RoundedCornerShape(Nocturne.radii.sm)),
    )
}

private fun parseHex(hex: String): Color? {
    val clean = hex.removePrefix("#")
    if (clean.length != 6) return null
    val value = clean.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or value)
}

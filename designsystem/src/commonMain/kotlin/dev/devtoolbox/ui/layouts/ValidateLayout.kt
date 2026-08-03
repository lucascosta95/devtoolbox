package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.Divider
import dev.devtoolbox.ds.components.LabelValueRow
import dev.devtoolbox.ds.components.SecondaryButton
import dev.devtoolbox.ds.components.StatusBadge
import dev.devtoolbox.ds.components.Text

/**
 * Arquétipo `validate`: o valor com o badge de status e o detalhamento abaixo.
 *
 * O status é anunciado por texto no badge, não só por cor — o DS é mono-accent e cor
 * sozinha não pode carregar significado.
 */
@Composable
fun ValidateLayout(
    body: ToolBody.Validate,
    onTryAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Nocturne.space.sm),
            horizontalArrangement = Arrangement.End,
        ) {
            SecondaryButton("Testar outro exemplo", onClick = onTryAnother)
        }

        Card {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Nocturne.space.md),
            ) {
                Text(
                    body.value.ifEmpty { "—" },
                    style = Nocturne.type.validatedValue,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(body.valid)
            }

            if (body.rows.isNotEmpty()) {
                Box(Modifier.padding(top = Nocturne.space.xs)) { Divider() }
                for ((index, row) in body.rows.withIndex()) {
                    LabelValueRow(row.label, row.value)
                    if (index != body.rows.lastIndex) Divider()
                }
            }
        }
    }
}

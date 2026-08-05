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
import androidx.compose.ui.text.style.TextOverflow
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.Divider
import dev.devtoolbox.ds.components.LabelValueRow
import dev.devtoolbox.ds.components.SecondaryButton
import dev.devtoolbox.ds.components.StatusBadge
import dev.devtoolbox.ds.components.Text

@Composable
fun ValidateLayout(
    body: ToolBody.Validate,
    toolId: String,
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
                ) {
                    Text(
                        body.value.ifEmpty { "—" },
                        style = Nocturne.type.validatedValue,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (body.value.isNotEmpty()) {
                        CopyButton(
                            body.value,
                            "$toolId-value",
                            contentDescription = "Copiar valor validado",
                        )
                    }
                }
                StatusBadge(body.valid)
            }

            if (body.rows.isNotEmpty()) {
                Box(Modifier.padding(top = Nocturne.space.xs)) { Divider() }
                for ((index, row) in body.rows.withIndex()) {
                    LabelValueRow(
                        label = row.label,
                        value = row.value,
                        trailing = {
                            CopyButton(
                                row.value,
                                "$toolId-row$index",
                                contentDescription = "Copiar ${row.label}",
                            )
                        },
                    )
                    if (index != body.rows.lastIndex) Divider()
                }
            }
        }
    }
}

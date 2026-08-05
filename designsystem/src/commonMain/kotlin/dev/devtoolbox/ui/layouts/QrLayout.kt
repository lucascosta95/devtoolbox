package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.Ramp
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.Text

@Composable
fun QrLayout(body: ToolBody.Qr, toolId: String, modifier: Modifier = Modifier) {
    val cellSize = 7.dp
    val quietZone = cellSize * 4
    val moduleCount = body.modules.size
    val boardSize = cellSize * moduleCount + quietZone * 2

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(boardSize)
                .clip(RoundedCornerShape(Nocturne.radii.sm))
                .background(Ramp.neutral100)
                .padding(quietZone),
        ) {
            Canvas(Modifier.size(cellSize * moduleCount)) {
                val cell = size.width / moduleCount
                for (row in body.modules.indices) {
                    for (col in body.modules[row].indices) {
                        if (!body.modules[row][col]) continue
                        drawRect(
                            color = Ramp.neutral900,
                            topLeft = Offset(col * cell, row * cell),
                            size = Size(cell + 0.5f, cell + 0.5f),
                        )
                    }
                }
            }
        }

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
        ) {
            Card {
                CardKicker("Conteúdo") { CopyButton(body.value, "$toolId-link", label = "Copiar link") }
                Box(Modifier.padding(top = Nocturne.space.xs)) {
                    Text(body.value, style = Nocturne.type.mono)
                }
            }
            Text(
                "${moduleCount}×$moduleCount módulos · correção de erros nível M",
                style = Nocturne.type.label,
                color = Nocturne.colors.text(0.5f),
            )
        }
    }
}

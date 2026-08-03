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

/**
 * Arquétipo `qr`: o código à esquerda e o conteúdo à direita.
 *
 * O QR é desenhado em `Canvas` a partir da matriz de módulos do core — sem imagem intermediária.
 * O fundo é sempre claro (`neutral-100`) com módulos escuros, porque leitores esperam esse
 * contraste; inverter no tema escuro quebraria a leitura.
 */
@Composable
fun QrLayout(body: ToolBody.Qr, toolId: String, modifier: Modifier = Modifier) {
    // Módulo menor = zona de silêncio menor, já que ela é 4 × módulo.
    val cellSize = 7.dp
    // A norma exige uma zona de silêncio de **4 módulos**; sem ela muitos leitores não
    // encontram os padrões de localização. É o mínimo — não dá para apertar mais. Como a
    // margem é proporcional ao módulo, o jeito de deixá-la discreta é o módulo menor.
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
                // Sem borda: com a moldura, a zona de silêncio lia como um quadro grosso em
                // vez de margem do próprio código.
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
                            // +0.5 evita fresta entre módulos quando `cell` não é inteiro.
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

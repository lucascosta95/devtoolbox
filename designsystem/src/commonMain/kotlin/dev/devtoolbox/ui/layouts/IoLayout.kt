package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.CodeTextField
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.PhosphorIcon
import dev.devtoolbox.ds.components.Text

/** Altura mínima dos dois cards — mantém o par estável enquanto a saída cresce. */
private val CARD_MIN_HEIGHT = 180.dp

/**
 * Arquétipo `io`: dois cards lado a lado com a seta entre eles.
 *
 * O card de entrada é **editável** — no protótipo era estático; aqui é a entrada real.
 */
@Composable
fun IoLayout(
    body: ToolBody.Io,
    toolId: String,
    inputText: String,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(Modifier.weight(1f).fillMaxHeight()) {
            CardKicker(body.inputLabel) {
                CopyButton(inputText, "$toolId-in", contentDescription = "Copiar entrada")
            }
            Box(Modifier.padding(top = Nocturne.space.xs)) {
                // O campo lê a entrada **ao vivo** do estado, não a saída debounced —
                // senão a digitação ficaria 150 ms atrasada.
                CodeTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = "Cole ou digite aqui…",
                    minHeight = CARD_MIN_HEIGHT,
                )
            }
        }

        PhosphorIcon("arrow-right", size = 18.dp, tint = Nocturne.colors.text(0.45f))

        Card(Modifier.weight(1f).fillMaxHeight()) {
            CardKicker(body.outputLabel) {
                CopyButton(body.output, "$toolId-out", label = "Copiar")
            }
            Column(Modifier.padding(top = Nocturne.space.xs)) {
                Text(
                    body.output,
                    style = Nocturne.type.mono,
                    color = Nocturne.colors.text(0.9f),
                )
            }
        }
    }
}

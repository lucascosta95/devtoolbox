package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.util.CodePoints
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.OutlinedButton
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ds.components.TextField
import dev.devtoolbox.ui.panels.InputPanel
import dev.devtoolbox.ui.panels.LocalPanelStore
import dev.devtoolbox.ui.panels.OutputPanel
import dev.devtoolbox.ui.panels.PanelEdges
import dev.devtoolbox.ui.panels.ResizableBlock
import dev.devtoolbox.ui.panels.SegmentHighlight

private val STRING_HEIGHT = 120.dp

private val RESULT_HEIGHT = 120.dp

private val INDEX_FIELD_HEIGHT = 36.dp

@Composable
fun SubstringLayout(
    body: ToolBody.Substring,
    toolId: String,
    input: ToolInput.Slice,
    onInputChange: (ToolInput.Slice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Nocturne.colors
    val store = LocalPanelStore.current
    val stringBlock = store.block("$toolId-string", STRING_HEIGHT)
    val resultBlock = store.block("$toolId-result", RESULT_HEIGHT)
    val empty = body.result.isEmpty()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
        ResizableBlock(stringBlock) {
            InputPanel(
                kicker = "String · ${body.length} caracteres",
                value = input.text,
                onValueChange = { onInputChange(input.copy(text = it)) },
                block = stringBlock,
                slot = "string",
                modifier = Modifier.fillMaxWidth(),
                placeholder = "cole ou digite a string aqui…",
                edges = PanelEdges(block = stringBlock, column = store.content),
                visualTransformation = SegmentHighlight(
                    segments = body.segments,
                    matched = colors.onAccentSurface,
                    highlight = colors.accentSurface,
                    dimmed = colors.text(0.4f),
                ),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
            IndexCard(
                kicker = "Início",
                value = input.start,
                onValueChange = { onInputChange(input.copy(start = it)) },
                placeholder = "1",
                modifier = Modifier.weight(1f),
            )
            IndexCard(
                kicker = "Comprimento",
                value = input.length,
                onValueChange = { onInputChange(input.copy(length = it)) },
                placeholder = "até o fim",
                modifier = Modifier.weight(1f),
            )
            ModesCard(
                multiLine = input.multiLine,
                reverse = input.reverse,
                onMultiLineChange = { onInputChange(input.copy(multiLine = it)) },
                onReverseChange = { onInputChange(input.copy(reverse = it)) },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            body.warning ?: "A posição 1 é o primeiro caractere e -1 o último. Comprimento negativo " +
                "recorta para a esquerda; vazio vai até o fim e \"-\" até o começo.",
            style = Nocturne.type.tag,
            color = if (body.warning != null) colors.accent else colors.text(0.5f),
        )

        ResizableBlock(resultBlock) {
            OutputPanel(
                kicker = "Resultado · ${CodePoints.count(body.result)} caracteres",
                text = AnnotatedString(if (empty) "Nada extraído" else body.result),
                block = resultBlock,
                slot = "result",
                modifier = Modifier.fillMaxWidth(),
                color = if (empty) colors.text(0.45f) else colors.text(0.9f),
                edges = PanelEdges(block = resultBlock, column = store.content),
                trailing = if (empty) {
                    null
                } else {
                    {
                        CopyButton(
                            body.result,
                            "$toolId-result",
                            contentDescription = "Copiar resultado",
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun IndexCard(
    kicker: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier) {
        CardKicker(kicker)
        Box(Modifier.padding(top = Nocturne.space.xs)) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                framed = false,
                textStyle = Nocturne.type.mono,
            )
        }
    }
}

@Composable
private fun ModesCard(
    multiLine: Boolean,
    reverse: Boolean,
    onMultiLineChange: (Boolean) -> Unit,
    onReverseChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier) {
        CardKicker("Modos")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
            modifier = Modifier
                .padding(top = Nocturne.space.xs)
                .defaultMinSize(minHeight = INDEX_FIELD_HEIGHT),
        ) {
            ModeToggle("Por linha", multiLine, onMultiLineChange)
            ModeToggle("Inverter", reverse, onReverseChange)
        }
    }
}

@Composable
private fun ModeToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = Nocturne.colors
    OutlinedButton(
        label = label,
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.semantics { toggleableState = ToggleableState(checked) },
        icon = "check-circle",
        iconFill = checked,
        accentBorder = checked,
        contentColor = if (checked) colors.accent else colors.text(0.7f),
        textStyle = Nocturne.type.tag,
    )
}

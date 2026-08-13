package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.PhosphorIcon
import dev.devtoolbox.ui.panels.COPY_LABEL_RESERVE
import dev.devtoolbox.ui.panels.InputPanel
import dev.devtoolbox.ui.panels.LocalPanelStore
import dev.devtoolbox.ui.panels.MIN_IO_PANEL_HEIGHT
import dev.devtoolbox.ui.panels.OutputPanel
import dev.devtoolbox.ui.panels.PanelEdges
import dev.devtoolbox.ui.panels.ResizableBlock
import dev.devtoolbox.ui.panels.ResizableRow
import dev.devtoolbox.ui.panels.SplitSide

private val IO_HEIGHT = 260.dp

@Composable
fun IoLayout(
    body: ToolBody.Io,
    toolId: String,
    inputText: String,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = LocalPanelStore.current
    val block = store.block("$toolId-io", IO_HEIGHT, minHeight = MIN_IO_PANEL_HEIGHT)

    ResizableBlock(block, modifier) {
        ResizableRow(
            block = block,
            divider = {
                PhosphorIcon("arrow-right", size = 18.dp, tint = Nocturne.colors.text(0.45f))
            },
            left = {
                InputPanel(
                    kicker = body.inputLabel,
                    value = inputText,
                    onValueChange = onInputChange,
                    block = block,
                    slot = "in",
                    modifier = Modifier.fillMaxWidth(),
                    edges = PanelEdges(block = block, splitSide = SplitSide.End),
                    trailing = {
                        CopyButton(inputText, "$toolId-in", contentDescription = "Copiar entrada")
                    },
                )
            },
            right = {
                OutputPanel(
                    kicker = body.outputLabel,
                    text = AnnotatedString(body.output),
                    block = block,
                    slot = "out",
                    modifier = Modifier.fillMaxWidth(),
                    trailingReserve = COPY_LABEL_RESERVE,
                    edges = PanelEdges(
                        block = block,
                        splitSide = SplitSide.Start,
                        column = store.content,
                    ),
                    trailing = { CopyButton(body.output, "$toolId-out", label = "Copiar") },
                )
            },
        )
    }
}

package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.devtoolbox.core.DiffKind
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ui.panels.COPY_LABEL_RESERVE
import dev.devtoolbox.ui.panels.LocalPanelStore
import dev.devtoolbox.ui.panels.PanelCard
import dev.devtoolbox.ui.panels.PanelEdges
import dev.devtoolbox.ui.panels.ResizableBlock

private val DIFF_HEIGHT = 280.dp

@Composable
fun DiffLayout(body: ToolBody.Diff, toolId: String, modifier: Modifier = Modifier) {
    val colors = Nocturne.colors
    val density = LocalDensity.current
    val store = LocalPanelStore.current
    val block = store.block("$toolId-diff", DIFF_HEIGHT)

    ResizableBlock(block, modifier) {
        PanelCard(
            kicker = "Diferenças",
            height = block.height,
            modifier = Modifier.fillMaxWidth(),
            trailingReserve = COPY_LABEL_RESERVE,
            edges = PanelEdges(block = block, column = store.content),
            trailing = { CopyButton(body.asPatch(), "$toolId-diff", label = "Copiar") },
        ) {
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .width(IntrinsicSize.Max)
                        .onGloballyPositioned {
                            block.reportContentHeight(
                                "diff",
                                with(density) { it.size.height.toDp() },
                            )
                        },
                ) {
                    for (line in body.lines) {
                        val (background, color, decoration) = when (line.kind) {
                            DiffKind.Add -> Triple(colors.accentSurface, colors.onAccentSurface, null)
                            DiffKind.Del -> Triple(
                                colors.mutedSurface,
                                colors.text(0.55f),
                                TextDecoration.LineThrough,
                            )
                            DiffKind.Same -> Triple(Color.Transparent, colors.text(0.85f), null)
                        }
                        val prefix = when (line.kind) {
                            DiffKind.Add -> "+"
                            DiffKind.Del -> "−"
                            DiffKind.Same -> " "
                        }
                        Text(
                            "$prefix ${line.text}",
                            style = Nocturne.type.mono
                                .copy(lineHeight = 24.sp, textDecoration = decoration),
                            color = color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(background)
                                .padding(horizontal = Nocturne.space.xs),
                        )
                    }
                }
            }
        }
    }
}

private fun ToolBody.Diff.asPatch(): String = lines.joinToString("\n") { line ->
    val prefix = when (line.kind) {
        DiffKind.Add -> "+"
        DiffKind.Del -> "-"
        DiffKind.Same -> " "
    }
    "$prefix${line.text}"
}

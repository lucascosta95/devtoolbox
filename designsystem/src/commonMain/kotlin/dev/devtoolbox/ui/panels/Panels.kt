package dev.devtoolbox.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.GhostButton
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ds.focusRing
import dev.devtoolbox.ui.horizontalResizeCursor
import dev.devtoolbox.ui.neswResizeCursor
import dev.devtoolbox.ui.nwseResizeCursor
import dev.devtoolbox.ui.verticalResizeCursor

val COPY_ICON_RESERVE = 34.dp

val COPY_LABEL_RESERVE = 92.dp

private val EDGE_BAND = 8.dp

private val CORNER_BAND = 13.dp

private val HANDLE_HEIGHT = 14.dp

private val GRIP_WIDTH = 46.dp

private val GRIP_HEIGHT = 3.dp

private val DIVIDER_WIDTH = 26.dp

private val JWT_DIVIDER_WIDTH = 3.dp

private val JWT_DIVIDER_HEIGHT = 34.dp

private const val CHROME_ALPHA = 0.22f

enum class SplitSide { Start, End }

@Immutable
class PanelEdges(
    val block: PanelBlock? = null,
    val splitSide: SplitSide? = null,
    val column: ContentColumn? = null,
)

@Composable
fun ResizableBlock(
    block: PanelBlock,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        content()
        PanelResizeHandle(block)
    }
}

@Composable
fun ResizableRow(
    block: PanelBlock,
    modifier: Modifier = Modifier,
    divider: @Composable () -> Unit = { PanelBarDivider() },
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(block.height)
            .onGloballyPositioned {
                val panels = it.size.width - with(density) { DIVIDER_WIDTH.roundToPx() }
                block.reportRowWidth(with(density) { panels.toDp() })
            },
    ) {
        Box(Modifier.weight(block.split).fillMaxHeight()) { left() }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(DIVIDER_WIDTH).fillMaxHeight(),
        ) {
            ResizeBand(
                modifier = Modifier.fillMaxSize(),
                cursor = horizontalResizeCursor,
                onDrag = { block.splitByWidth(with(density) { it.x.toDp() }) },
                onDoubleClick = { block.resetSplit() },
            )
            divider()
        }

        Box(Modifier.weight(1f - block.split).fillMaxHeight()) { right() }
    }
}

@Composable
fun PanelBarDivider() {
    Box(
        Modifier
            .size(JWT_DIVIDER_WIDTH, JWT_DIVIDER_HEIGHT)
            .clip(RoundedCornerShape(Nocturne.radii.sm))
            .background(Nocturne.colors.text(CHROME_ALPHA)),
    )
}

@Composable
private fun ResizeBand(
    modifier: Modifier,
    cursor: PointerIcon,
    onDrag: (Offset) -> Unit,
    onDoubleClick: (() -> Unit)? = null,
) {
    val store = LocalPanelStore.current
    val currentDrag by rememberUpdatedState(onDrag)
    val currentDoubleClick by rememberUpdatedState(onDoubleClick)

    Box(
        modifier
            .pointerHoverIcon(cursor)
            .pointerInput(cursor) {
                detectDragGestures(
                    onDragStart = { store.dragCursor = cursor },
                    onDragEnd = { store.dragCursor = null },
                    onDragCancel = { store.dragCursor = null },
                ) { change, dragAmount ->
                    change.consume()
                    currentDrag(dragAmount)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = {}, onDoubleTap = { currentDoubleClick?.invoke() })
            },
    )
}

@Composable
private fun BoxScope.PanelEdgeBands(edges: PanelEdges) {
    val density = LocalDensity.current
    val block = edges.block
    val column = edges.column
    val splitSide = edges.splitSide

    val startCorner = block != null && splitSide == SplitSide.Start
    val endCorner = block != null && (splitSide == SplitSide.End || column != null)

    if (block != null) {
        ResizeBand(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(EDGE_BAND)
                .padding(
                    start = if (startCorner) CORNER_BAND else 0.dp,
                    end = if (endCorner) CORNER_BAND else 0.dp,
                ),
            cursor = verticalResizeCursor,
            onDrag = { block.resizeBy(with(density) { it.y.toDp() }) },
            onDoubleClick = { block.fitToContent() },
        )
    }

    if (splitSide != null && block != null) {
        ResizeBand(
            modifier = Modifier
                .align(if (splitSide == SplitSide.Start) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .width(EDGE_BAND)
                .padding(bottom = CORNER_BAND),
            cursor = horizontalResizeCursor,
            onDrag = { block.splitByWidth(with(density) { it.x.toDp() }) },
            onDoubleClick = { block.resetSplit() },
        )
    }

    if (column != null) {
        ResizeBand(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(EDGE_BAND)
                .padding(bottom = if (block != null) CORNER_BAND else 0.dp),
            cursor = horizontalResizeCursor,
            onDrag = { column.resizeBy(with(density) { it.x.toDp() }) },
            onDoubleClick = { column.reset() },
        )
    }

    if (startCorner && block != null) {
        ResizeBand(
            modifier = Modifier.align(Alignment.BottomStart).size(CORNER_BAND),
            cursor = neswResizeCursor,
            onDrag = {
                with(density) {
                    block.resizeBy(it.y.toDp())
                    block.splitByWidth(it.x.toDp())
                }
            },
        )
    }

    if (endCorner && block != null) {
        ResizeBand(
            modifier = Modifier.align(Alignment.BottomEnd).size(CORNER_BAND),
            cursor = nwseResizeCursor,
            onDrag = {
                with(density) {
                    block.resizeBy(it.y.toDp())
                    if (column != null) column.resizeBy(it.x.toDp()) else block.splitByWidth(it.x.toDp())
                }
            },
        )
    }
}

@Composable
private fun PanelResizeHandle(block: PanelBlock) {
    val density = LocalDensity.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f).height(HANDLE_HEIGHT),
        ) {
            ResizeBand(
                modifier = Modifier.fillMaxSize(),
                cursor = verticalResizeCursor,
                onDrag = { block.resizeBy(with(density) { it.y.toDp() }) },
                onDoubleClick = { block.fitToContent() },
            )
            Box(
                Modifier
                    .size(GRIP_WIDTH, GRIP_HEIGHT)
                    .clip(RoundedCornerShape(Nocturne.radii.sm))
                    .background(Nocturne.colors.text(CHROME_ALPHA)),
            )
        }

        GhostButton(
            label = "Ajustar",
            onClick = { block.fitToContent() },
            icon = "arrows-out-line-vertical",
            iconSize = 11.5.dp,
            contentDescription = "Ajustar a altura ao conteúdo",
        )
    }
}

@Composable
fun OutputPanel(
    kicker: String,
    text: AnnotatedString,
    block: PanelBlock,
    slot: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Nocturne.type.mono,
    color: Color = Nocturne.colors.text(0.9f),
    trailingReserve: Dp = COPY_ICON_RESERVE,
    edges: PanelEdges = PanelEdges(),
    trailing: @Composable (() -> Unit)? = null,
) {
    PanelCard(
        kicker = kicker,
        height = block.height,
        modifier = modifier,
        trailingReserve = trailingReserve,
        edges = edges,
        trailing = trailing,
    ) {
        val density = LocalDensity.current
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                text,
                style = style,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        block.reportContentHeight(slot, with(density) { it.size.height.toDp() })
                    },
            )
        }
    }
}

@Composable
fun InputPanel(
    kicker: String,
    value: String,
    onValueChange: (String) -> Unit,
    block: PanelBlock,
    slot: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Cole ou digite aqui…",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingReserve: Dp = COPY_ICON_RESERVE,
    edges: PanelEdges = PanelEdges(),
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = Nocturne.colors
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    val selectionColors = TextSelectionColors(
        handleColor = colors.accent,
        backgroundColor = colors.accent.copy(alpha = 0.3f),
    )

    PanelCard(
        kicker = kicker,
        height = block.height,
        modifier = modifier,
        focused = focused,
        onSurfaceClick = { runCatching { focusRequester.requestFocus() } },
        trailingReserve = trailingReserve,
        edges = edges,
        trailing = trailing,
    ) {
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            Box(Modifier.fillMaxSize()) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = Nocturne.type.mono, color = colors.text(0.4f))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = Nocturne.type.mono.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    visualTransformation = visualTransformation,
                    onTextLayout = {
                        block.reportContentHeight(slot, with(density) { it.size.height.toDp() })
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused },
                )
            }
        }
    }
}

@Composable
fun PanelCard(
    kicker: String,
    height: Dp,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    onSurfaceClick: (() -> Unit)? = null,
    trailingReserve: Dp = 0.dp,
    edges: PanelEdges = PanelEdges(),
    trailing: @Composable (() -> Unit)? = null,
    body: @Composable () -> Unit,
) {
    Box(modifier.height(height)) {
        Card(
            Modifier
                .fillMaxSize()
                .focusRing(focused, Nocturne.radii.md)
                .then(
                    if (onSurfaceClick != null) {
                        Modifier
                            .pointerHoverIcon(PointerIcon.Text)
                            .pointerInput(onSurfaceClick) {
                                detectTapGestures { onSurfaceClick() }
                            }
                    } else {
                        Modifier
                    },
                ),
        ) {
            CardKicker(kicker, trailingReserve = trailingReserve, trailing = trailing)
            Box(Modifier.fillMaxWidth().weight(1f).padding(top = Nocturne.space.xs)) {
                body()
            }
        }

        PanelEdgeBands(edges)
    }
}

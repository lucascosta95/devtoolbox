package dev.devtoolbox.ui.panels

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val MIN_PANEL_HEIGHT = 90.dp

val MIN_IO_PANEL_HEIGHT = 150.dp

val MAX_PANEL_HEIGHT = 900.dp

val FIT_PADDING = 44.dp

val MIN_SPLIT_PANEL_WIDTH = 230.dp

val INITIAL_CONTENT_WIDTH = 760.dp

val MIN_CONTENT_WIDTH = 460.dp

const val EVEN_SPLIT = 0.5f

fun clampPanelHeight(height: Dp, minHeight: Dp = MIN_PANEL_HEIGHT): Dp =
    height.coerceIn(minHeight, maxOf(minHeight, MAX_PANEL_HEIGHT))

fun clampSplit(fraction: Float, rowWidth: Dp): Float {
    val smallest = if (rowWidth > 0.dp) MIN_SPLIT_PANEL_WIDTH / rowWidth else 0f
    val largest = 1f - smallest
    return if (smallest > largest) EVEN_SPLIT else fraction.coerceIn(smallest, largest)
}

fun fitPanelHeight(contentHeight: Dp, minHeight: Dp = MIN_PANEL_HEIGHT): Dp =
    clampPanelHeight(contentHeight + FIT_PADDING, minHeight)

fun clampContentWidth(width: Dp, available: Dp): Dp =
    width.coerceIn(MIN_CONTENT_WIDTH, maxOf(MIN_CONTENT_WIDTH, available))

@Stable
class PanelBlock(
    initialHeight: Dp,
    val minHeight: Dp = MIN_PANEL_HEIGHT,
    initialSplit: Float = EVEN_SPLIT,
) {

    var height: Dp by mutableStateOf(clampPanelHeight(initialHeight, minHeight))
        private set

    var rowWidth: Dp by mutableStateOf(0.dp)
        private set

    var split: Float by mutableStateOf(clampSplit(initialSplit, 0.dp))
        private set

    private val contentHeights = HashMap<String, Dp>()

    fun resizeTo(value: Dp) {
        height = clampPanelHeight(value, minHeight)
    }

    fun resizeBy(delta: Dp) = resizeTo(height + delta)

    fun splitTo(fraction: Float) {
        split = clampSplit(fraction, rowWidth)
    }

    fun splitBy(delta: Float) = splitTo(split + delta)

    fun splitByWidth(delta: Dp) {
        if (rowWidth > 0.dp) splitBy(delta / rowWidth)
    }

    fun resetSplit() = splitTo(EVEN_SPLIT)

    fun reportRowWidth(value: Dp) {
        if (value == rowWidth) return
        rowWidth = value
        split = clampSplit(split, value)
    }

    fun reportContentHeight(slot: String, value: Dp) {
        contentHeights[slot] = value
    }

    val tallestContent: Dp? get() = contentHeights.values.maxOrNull()

    fun fitToContent() {
        resizeTo(fitPanelHeight(tallestContent ?: return, minHeight))
    }
}

@Stable
class ContentColumn {

    var available: Dp by mutableStateOf(Dp.Infinity)
        private set

    var width: Dp by mutableStateOf(INITIAL_CONTENT_WIDTH)
        private set

    fun reportAvailable(value: Dp) {
        available = value
        width = clampContentWidth(width, value)
    }

    fun resizeTo(value: Dp) {
        width = clampContentWidth(value, available)
    }

    fun resizeBy(delta: Dp) = resizeTo(width + delta)

    fun reset() = resizeTo(INITIAL_CONTENT_WIDTH)
}

@Stable
class PanelStore {

    private val blocks = LinkedHashMap<String, PanelBlock>()

    val content = ContentColumn()

    var dragCursor: PointerIcon? by mutableStateOf(null)

    fun block(
        key: String,
        initialHeight: Dp,
        minHeight: Dp = MIN_PANEL_HEIGHT,
        initialSplit: Float = EVEN_SPLIT,
    ): PanelBlock = blocks.getOrPut(key) { PanelBlock(initialHeight, minHeight, initialSplit) }
}

val LocalPanelStore = staticCompositionLocalOf { PanelStore() }

package dev.devtoolbox.ui

import androidx.compose.ui.unit.dp
import dev.devtoolbox.ui.panels.EVEN_SPLIT
import dev.devtoolbox.ui.panels.INITIAL_CONTENT_WIDTH
import dev.devtoolbox.ui.panels.MAX_PANEL_HEIGHT
import dev.devtoolbox.ui.panels.MIN_CONTENT_WIDTH
import dev.devtoolbox.ui.panels.MIN_IO_PANEL_HEIGHT
import dev.devtoolbox.ui.panels.MIN_PANEL_HEIGHT
import dev.devtoolbox.ui.panels.MIN_SPLIT_PANEL_WIDTH
import dev.devtoolbox.ui.panels.ContentColumn
import dev.devtoolbox.ui.panels.PanelBlock
import dev.devtoolbox.ui.panels.PanelStore
import dev.devtoolbox.ui.panels.clampContentWidth
import dev.devtoolbox.ui.panels.clampPanelHeight
import dev.devtoolbox.ui.panels.clampSplit
import dev.devtoolbox.ui.panels.fitPanelHeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val TOLERANCE = 0.0001f

class PanelSizingTest {

    @Test
    fun heightIsClampedToTheExtremes() {
        assertEquals(MIN_PANEL_HEIGHT, clampPanelHeight(0.dp))
        assertEquals(MIN_PANEL_HEIGHT, clampPanelHeight((-500).dp))
        assertEquals(MAX_PANEL_HEIGHT, clampPanelHeight(5_000.dp))
        assertEquals(260.dp, clampPanelHeight(260.dp))
    }

    @Test
    fun theInputOutputBlockUsesTheTallerMinimum() {
        assertEquals(MIN_IO_PANEL_HEIGHT, clampPanelHeight(20.dp, MIN_IO_PANEL_HEIGHT))
        assertEquals(MAX_PANEL_HEIGHT, clampPanelHeight(5_000.dp, MIN_IO_PANEL_HEIGHT))

        val block = PanelBlock(260.dp, minHeight = MIN_IO_PANEL_HEIGHT)
        block.resizeBy((-1_000).dp)
        assertEquals(MIN_IO_PANEL_HEIGHT, block.height)
    }

    @Test
    fun draggingPastTheLimitsStopsAtTheExtremes() {
        val block = PanelBlock(260.dp)

        block.resizeBy((-1_000).dp)
        assertEquals(MIN_PANEL_HEIGHT, block.height)

        block.resizeBy(10_000.dp)
        assertEquals(MAX_PANEL_HEIGHT, block.height)
    }

    @Test
    fun theSplitKeepsBothPanelsAboveTheMinimumWidthOnAWideRow() {
        val row = 1_000.dp

        assertEquals(MIN_SPLIT_PANEL_WIDTH / row, clampSplit(0f, row), TOLERANCE)
        assertEquals(1f - MIN_SPLIT_PANEL_WIDTH / row, clampSplit(1f, row), TOLERANCE)
        assertEquals(EVEN_SPLIT, clampSplit(EVEN_SPLIT, row), TOLERANCE)

        assertTrue(row * clampSplit(0f, row) >= MIN_SPLIT_PANEL_WIDTH)
        assertTrue(row * (1f - clampSplit(1f, row)) >= MIN_SPLIT_PANEL_WIDTH)
    }

    @Test
    fun theSplitKeepsBothPanelsAboveTheMinimumWidthOnANarrowRow() {
        val row = 500.dp

        val smallest = clampSplit(0f, row)
        val largest = clampSplit(1f, row)

        assertEquals(0.46f, smallest, TOLERANCE)
        assertEquals(0.54f, largest, TOLERANCE)
        assertTrue(row * smallest >= MIN_SPLIT_PANEL_WIDTH)
        assertTrue(row * (1f - largest) >= MIN_SPLIT_PANEL_WIDTH)
    }

    @Test
    fun aRowTooNarrowForTwoPanelsFallsBackToTheCentre() {
        assertEquals(EVEN_SPLIT, clampSplit(0.1f, 400.dp), TOLERANCE)
        assertEquals(EVEN_SPLIT, clampSplit(0.9f, 400.dp), TOLERANCE)
    }

    @Test
    fun draggingTheDividerStopsAtTheMinimumPanelWidth() {
        val block = PanelBlock(260.dp)
        block.reportRowWidth(1_000.dp)

        block.splitByWidth((-900).dp)
        assertEquals(MIN_SPLIT_PANEL_WIDTH / 1_000.dp, block.split, TOLERANCE)

        block.splitByWidth(900.dp)
        assertEquals(1f - MIN_SPLIT_PANEL_WIDTH / 1_000.dp, block.split, TOLERANCE)
    }

    @Test
    fun theDividerGoesBackToTheCentre() {
        val block = PanelBlock(260.dp)
        block.reportRowWidth(1_000.dp)
        block.splitByWidth(200.dp)

        block.resetSplit()

        assertEquals(EVEN_SPLIT, block.split, TOLERANCE)
    }

    @Test
    fun aNarrowerWindowPullsTheSplitBackInsideTheLimits() {
        val block = PanelBlock(260.dp)
        block.reportRowWidth(1_000.dp)
        block.splitByWidth(250.dp)
        assertTrue(block.split > 0.7f)

        block.reportRowWidth(500.dp)

        assertEquals(0.54f, block.split, TOLERANCE)
    }

    @Test
    fun theContentColumnIsClampedBetweenTheMinimumAndWhatIsAvailable() {
        assertEquals(MIN_CONTENT_WIDTH, clampContentWidth(100.dp, 1_200.dp))
        assertEquals(1_200.dp, clampContentWidth(5_000.dp, 1_200.dp))
        assertEquals(MIN_CONTENT_WIDTH, clampContentWidth(600.dp, 300.dp))
    }

    @Test
    fun draggingTheContentColumnStopsAtTheMinimumAndAtTheAvailableWidth() {
        val column = ContentColumn()
        column.reportAvailable(1_000.dp)

        column.resizeBy((-2_000).dp)
        assertEquals(MIN_CONTENT_WIDTH, column.width)

        column.resizeBy(5_000.dp)
        assertEquals(1_000.dp, column.width)
    }

    @Test
    fun theContentColumnGoesBackTo760() {
        val column = ContentColumn()
        column.reportAvailable(1_200.dp)
        column.resizeTo(1_100.dp)

        column.reset()

        assertEquals(INITIAL_CONTENT_WIDTH, column.width)
    }

    @Test
    fun aNarrowerWindowShrinksTheContentColumn() {
        val column = ContentColumn()
        column.reportAvailable(1_200.dp)
        column.resizeTo(1_100.dp)

        column.reportAvailable(600.dp)

        assertEquals(600.dp, column.width)
    }

    @Test
    fun fittingSmallContentRespectsTheMinimumHeight() {
        val block = PanelBlock(400.dp)
        block.reportContentHeight("out", 12.dp)

        block.fitToContent()

        assertEquals(MIN_PANEL_HEIGHT, block.height)
    }

    @Test
    fun fittingSmallContentRespectsTheInputOutputMinimumHeight() {
        val block = PanelBlock(400.dp, minHeight = MIN_IO_PANEL_HEIGHT)
        block.reportContentHeight("out", 12.dp)

        block.fitToContent()

        assertEquals(MIN_IO_PANEL_HEIGHT, block.height)
    }

    @Test
    fun fittingLargeContentStopsAtTheMaximumHeight() {
        val block = PanelBlock(260.dp)
        block.reportContentHeight("out", 4_000.dp)

        block.fitToContent()

        assertEquals(MAX_PANEL_HEIGHT, block.height)
    }

    @Test
    fun fittingUsesTheTallestPanelOfTheBlockPlusThePadding() {
        val block = PanelBlock(260.dp)
        block.reportContentHeight("in", 150.dp)
        block.reportContentHeight("out", 300.dp)

        block.fitToContent()

        assertEquals(fitPanelHeight(300.dp), block.height)
        assertTrue(block.height > 300.dp, "a altura precisa caber o conteúdo mais alto")
    }

    @Test
    fun fittingWithoutMeasuredContentKeepsTheCurrentHeight() {
        val block = PanelBlock(260.dp)
        block.fitToContent()
        assertEquals(260.dp, block.height)
    }

    @Test
    fun theInitialHeightIsClampedToo() {
        assertEquals(MAX_PANEL_HEIGHT, PanelBlock(2_000.dp).height)
        assertEquals(MIN_PANEL_HEIGHT, PanelBlock(10.dp).height)
    }

    @Test
    fun theStoreKeepsOneBlockPerKeyForTheWholeSession() {
        val store = PanelStore()
        val first = store.block("json-io", 260.dp)
        first.resizeTo(420.dp)

        val again = store.block("json-io", 260.dp)

        assertSame(first, again)
        assertEquals(420.dp, again.height, "a altura é estado de sessão e sobrevive à troca de aba")
    }

    @Test
    fun theStoreKeepsOneContentColumnForTheWholeSession() {
        val store = PanelStore()
        store.content.reportAvailable(1_200.dp)
        store.content.resizeTo(900.dp)

        assertEquals(900.dp, store.content.width)
    }
}

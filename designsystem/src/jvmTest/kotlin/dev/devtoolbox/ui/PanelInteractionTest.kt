package dev.devtoolbox.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.AccentColor
import dev.devtoolbox.ds.NocturneTheme
import dev.devtoolbox.ds.ThemeMode
import dev.devtoolbox.ui.panels.InputPanel
import dev.devtoolbox.ui.panels.PanelBlock
import dev.devtoolbox.ui.panels.PanelEdges
import kotlin.test.Test

private const val PANEL = "panel"

private val PANEL_WIDTH = 300.dp

private val PANEL_HEIGHT = 200.dp

@OptIn(ExperimentalTestApi::class)
class PanelInteractionTest {

    @Test
    fun clickingTheEmptyAreaOfTheCardFocusesTheInput() {
        runComposeUiTest {
            setContent { Harness() }

            onNodeWithTag(PANEL).performMouseInput {
                moveTo(Offset(150f, 150f))
                press()
                release()
            }

            onNode(hasSetTextAction()).assertIsFocused()
        }
    }

    @Test
    fun clickingTheBottomResizeBandDoesNotFocusTheInput() {
        runComposeUiTest {
            setContent { Harness() }

            onNodeWithTag(PANEL).performMouseInput {
                moveTo(Offset(150f, 196f))
                press()
                release()
            }

            onNode(hasSetTextAction()).assertIsNotFocused()
        }
    }

    @Test
    fun draggingTheBottomResizeBandChangesTheHeightAndLeavesTheInputAlone() {
        runComposeUiTest {
            val block = PanelBlock(PANEL_HEIGHT)

            setContent { Harness(block) }

            onNodeWithTag(PANEL).performMouseInput {
                moveTo(Offset(150f, 196f))
                press()
                moveBy(Offset(0f, 60f))
                moveBy(Offset(0f, 60f))
                release()
            }

            onNode(hasSetTextAction()).assertIsNotFocused()
            assertHeightGrew(block)
        }
    }

    private fun assertHeightGrew(block: PanelBlock) {
        check(block.height > PANEL_HEIGHT) {
            "arrastar a borda inferior precisa aumentar a altura — ficou em ${block.height}"
        }
    }

    @Composable
    private fun Harness(block: PanelBlock = PanelBlock(PANEL_HEIGHT)) {
        var value by remember { mutableStateOf("") }
        val panel = remember { block }

        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            NocturneTheme(ThemeMode.Dark, AccentColor.Teal) {
                Box(Modifier.size(PANEL_WIDTH, PANEL_HEIGHT)) {
                    InputPanel(
                        kicker = "Entrada",
                        value = value,
                        onValueChange = { value = it },
                        block = panel,
                        slot = "in",
                        modifier = Modifier.fillMaxWidth().testTag(PANEL),
                        edges = PanelEdges(block = panel),
                    )
                }
            }
        }
    }
}

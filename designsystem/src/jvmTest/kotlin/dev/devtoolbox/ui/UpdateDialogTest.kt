package dev.devtoolbox.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import dev.devtoolbox.core.update.Release
import dev.devtoolbox.ds.AccentColor
import dev.devtoolbox.ds.NocturneTheme
import dev.devtoolbox.ds.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

private val RELEASE = Release(
    tag = "v9.0.0",
    url = "https://github.com/lucascosta95/devtoolbox/releases/tag/v9.0.0",
)

@OptIn(ExperimentalTestApi::class)
class UpdateDialogTest {

    @Test
    fun theDialogAnnouncesTheNewVersionAndTheThreeChoices() = runComposeUiTest {
        setContent { Harness(UpdateNotice.Available(RELEASE)) }

        onNodeWithText("DevToolbox v9.0.0").assertIsDisplayed()
        onNodeWithText("Ver no GitHub").assertIsDisplayed()
        onNodeWithText("Lembrar depois").assertIsDisplayed()
        onNodeWithText("Pular esta versão").assertIsDisplayed()
    }

    @Test
    fun seeOnGithubHandsBackTheRelease() = runComposeUiTest {
        val opened = mutableListOf<Release>()
        setContent { Harness(UpdateNotice.Available(RELEASE), onOpenRelease = opened::add) }

        onNodeWithText("Ver no GitHub").performClick()

        assertEquals(listOf(RELEASE), opened)
    }

    @Test
    fun remindLaterReportsBackOnce() = runComposeUiTest {
        var remindLater = 0
        setContent { Harness(UpdateNotice.Available(RELEASE), onRemindLater = { remindLater++ }) }

        onNodeWithText("Lembrar depois").performClick()

        assertEquals(1, remindLater)
    }

    @Test
    fun skipThisVersionHandsBackTheRelease() = runComposeUiTest {
        val skipped = mutableListOf<Release>()
        setContent { Harness(UpdateNotice.Available(RELEASE), onSkipVersion = skipped::add) }

        onNodeWithText("Pular esta versão").performClick()

        assertEquals(listOf(RELEASE), skipped)
    }

    @Test
    fun clickingTheScrimIsTheSameAsRemindLater() = runComposeUiTest {
        var remindLater = 0
        setContent { Harness(UpdateNotice.Available(RELEASE), onRemindLater = { remindLater++ }) }

        onRoot().performMouseInput {
            moveTo(Offset(4f, 4f))
            press()
            release()
        }

        assertEquals(1, remindLater)
    }

    @Test
    fun escapeIsTheSameAsRemindLater() = runComposeUiTest {
        var remindLater = 0
        setContent { Harness(UpdateNotice.Available(RELEASE), onRemindLater = { remindLater++ }) }

        onRoot().performKeyInput { pressKey(Key.Escape) }

        assertEquals(1, remindLater)
    }

    @Test
    fun theUpToDateVariantOnlyOffersToClose() = runComposeUiTest {
        var closed = 0
        setContent { Harness(UpdateNotice.UpToDate, onRemindLater = { closed++ }) }

        onNodeWithText("Você já está na versão mais recente.").assertIsDisplayed()
        onNodeWithText("Fechar").performClick()

        assertEquals(1, closed)
    }

    @Composable
    private fun Harness(
        notice: UpdateNotice,
        onOpenRelease: (Release) -> Unit = {},
        onRemindLater: () -> Unit = {},
        onSkipVersion: (Release) -> Unit = {},
    ) {
        NocturneTheme(ThemeMode.Dark, AccentColor.Teal) {
            UpdateDialog(
                notice = notice,
                onOpenRelease = onOpenRelease,
                onRemindLater = onRemindLater,
                onSkipVersion = onSkipVersion,
            )
        }
    }
}

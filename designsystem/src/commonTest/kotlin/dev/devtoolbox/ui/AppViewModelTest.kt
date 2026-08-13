package dev.devtoolbox.ui

import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.ds.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @Test
    fun favoritesToggleBothWays() = runTest {
        val vm = AppViewModel(backgroundScope)
        assertTrue("base64" in vm.state.value.favorites)

        vm.toggleFavorite("base64")
        assertTrue("base64" !in vm.state.value.favorites)

        vm.toggleFavorite("base64")
        assertTrue("base64" in vm.state.value.favorites)
    }

    @Test
    fun themeToggles() = runTest {
        val vm = AppViewModel(backgroundScope)
        assertEquals(ThemeMode.Dark, vm.state.value.theme)
        vm.toggleTheme()
        assertEquals(ThemeMode.Light, vm.state.value.theme)
    }

    @Test
    fun searchHidesFavorites() = runTest {
        val vm = AppViewModel(backgroundScope)
        vm.select("url")
        assertTrue(vm.state.value.favoriteTools.isNotEmpty())

        vm.onQueryChange("json")
        assertTrue(vm.state.value.favoriteTools.isEmpty())
        assertEquals(
            listOf("json", "json-string", "diff"),
            vm.state.value.categories.flatMap { it.second }.map { it.id },
        )
    }

    @Test
    fun outputFollowsInputAfterTheDebounce() = runTest {
        val vm = AppViewModel(backgroundScope)
        vm.updateInput("base64", ToolInput.Text("abc"))

        advanceTimeBy(INPUT_DEBOUNCE_MS + 50)

        val out = vm.output.value
        assertIs<ToolOutput.Success>(out)
        assertEquals("YWJj", (out.body as ToolBody.Io).output)
    }

    @Test
    fun switchingToolsDoesNotWaitForTheDebounce() = runTest {
        val vm = AppViewModel(backgroundScope)
        vm.select("json")

        advanceTimeBy(1)

        val out = vm.output.value
        assertIs<ToolOutput.Success>(out)
        assertTrue((out.body as ToolBody.Io).output.contains("\"name\": \"Ana Souza\""))
    }

    @Test
    fun resetInputRestoresTheExample() = runTest {
        val vm = AppViewModel(backgroundScope)
        vm.updateInput("base64", ToolInput.Text("outro"))
        assertEquals(ToolInput.Text("outro"), vm.state.value.currentInput)

        vm.resetInput("base64")
        assertEquals(vm.state.value.selectedTool.defaultInput, vm.state.value.currentInput)
    }
}

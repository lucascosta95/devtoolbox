package dev.devtoolbox.ui

import dev.devtoolbox.core.ToolRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationTest {

    @Test
    fun movingDownWalksTheSidebarOrder() = runTest {
        val vm = AppViewModel(backgroundScope)
        assertEquals(ToolRegistry.all[0].id, vm.state.value.selectedId)

        vm.move(1)
        assertEquals(ToolRegistry.all[1].id, vm.state.value.selectedId)

        vm.move(1)
        assertEquals(ToolRegistry.all[2].id, vm.state.value.selectedId)

        vm.move(-1)
        assertEquals(ToolRegistry.all[1].id, vm.state.value.selectedId)
    }

    @Test
    fun movingStopsAtTheEdges() = runTest {
        val vm = AppViewModel(backgroundScope)
        vm.move(-1)
        assertEquals(ToolRegistry.all.first().id, vm.state.value.selectedId)

        repeat(ToolRegistry.all.size + 5) { vm.move(1) }
        assertEquals(ToolRegistry.all.last().id, vm.state.value.selectedId)
    }

    @Test
    fun movingRespectsTheSearchFilter() = runTest {
        val vm = AppViewModel(backgroundScope)
        vm.onQueryChange("validators")

        val expected = ToolRegistry.search("validators")
        vm.move(1)
        assertEquals(expected.first().id, vm.state.value.selectedId)

        vm.move(1)
        assertEquals(expected[1].id, vm.state.value.selectedId)
    }

    @Test
    fun movingDoesNothingWhenSearchHasNoResults() = runTest {
        val vm = AppViewModel(backgroundScope)
        vm.onQueryChange("zzzz")
        val before = vm.state.value.selectedId
        vm.move(1)
        assertEquals(before, vm.state.value.selectedId)
    }

    @Test
    fun navigationFeedsTheRecentList() = runTest {
        val vm = AppViewModel(backgroundScope)
        vm.move(1)
        vm.move(1)
        assertEquals(listOf(ToolRegistry.all[2].id, ToolRegistry.all[1].id), vm.state.value.recent)
    }

    @Test
    fun clearQueryEmptiesTheSearch() = runTest {
        val vm = AppViewModel(backgroundScope)
        vm.onQueryChange("json")
        vm.clearQuery()
        assertEquals("", vm.state.value.query)
    }

    @Test
    fun favoriteShortcutTogglesTheSelectedTool() = runTest {
        val vm = AppViewModel(backgroundScope)
        vm.select("cron")
        vm.toggleFavoriteOfSelected()
        assertEquals(true, "cron" in vm.state.value.favorites)
        vm.toggleFavoriteOfSelected()
        assertEquals(false, "cron" in vm.state.value.favorites)
    }
}

package dev.devtoolbox.ui

import dev.devtoolbox.core.ToolRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationTest {

    @Test
    fun navigationStartsWithTheFavoritesThenTheCategories() = runTest {
        val vm = AppViewModel(backgroundScope)
        val order = vm.state.value.navigableTools.map { it.id }

        assertEquals(listOf("base64", "json"), order.take(2))
        assertEquals(ToolRegistry.all.size, order.size, "cada ferramenta aparece uma única vez")
    }

    @Test
    fun movingDownWalksTheSidebarOrder() = runTest {
        val vm = AppViewModel(backgroundScope)
        val order = vm.state.value.navigableTools.map { it.id }
        assertEquals(order[0], vm.state.value.selectedId)

        vm.move(1)
        assertEquals(order[1], vm.state.value.selectedId)

        vm.move(1)
        assertEquals(order[2], vm.state.value.selectedId)

        vm.move(-1)
        assertEquals(order[1], vm.state.value.selectedId)
    }

    @Test
    fun movingStopsAtTheEdges() = runTest {
        val vm = AppViewModel(backgroundScope)
        val order = vm.state.value.navigableTools

        vm.move(-1)
        assertEquals(order.first().id, vm.state.value.selectedId)

        repeat(order.size + 5) { vm.move(1) }
        assertEquals(order.last().id, vm.state.value.selectedId)
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

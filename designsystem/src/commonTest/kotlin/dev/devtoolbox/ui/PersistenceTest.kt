package dev.devtoolbox.ui

import dev.devtoolbox.core.persistence.InMemoryStateStore
import dev.devtoolbox.core.persistence.PersistedState
import dev.devtoolbox.core.persistence.StateCodec
import dev.devtoolbox.ds.AccentColor
import dev.devtoolbox.ds.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PersistenceTest {

    @Test
    fun roundTripsThroughJson() {
        val state = PersistedState(
            selectedId = "cron",
            favorites = listOf("base64", "json"),
            theme = "light",
            accent = "teal",
        )
        assertEquals(state, StateCodec.decode(StateCodec.encode(state)))
    }

    @Test
    fun corruptedFileDecodesToNullInsteadOfThrowing() {
        assertNull(StateCodec.decode("{ isso não é json"))
        assertNull(StateCodec.decode(""))
    }

    @Test
    fun unknownKeysAreIgnoredSoOlderFilesStillLoad() {
        val decoded = StateCodec.decode("""{"selected_id":"uuid","chave_do_futuro":42}""")
        assertEquals("uuid", decoded?.selectedId)
    }

    @Test
    fun savedStateIsAppliedOnStartup() {
        val saved = PersistedState(
            selectedId = "cron",
            favorites = listOf("uuid"),
            theme = "light",
        )
        val state = AppState().mergedWith(saved)
        assertEquals("cron", state.selectedId)
        assertEquals(setOf("uuid"), state.favorites)
        assertEquals(ThemeMode.Light, state.theme)
    }

    @Test
    fun unknownToolIdsAreDroppedWhenLoading() {
        val saved = PersistedState(
            selectedId = "ferramenta-removida",
            favorites = listOf("base64", "ferramenta-removida"),
        )
        val state = AppState().mergedWith(saved)
        assertEquals(AppState().selectedId, state.selectedId)
        assertEquals(setOf("base64"), state.favorites)
    }

    @Test
    fun aLegacyFileWithTheRecentKeyStillLoads() {
        val legacy = """
            {
              "selected_id": "cron",
              "favorites": ["base64"],
              "recent": ["cron", "uuid"],
              "theme": "light",
              "accent": "teal",
              "version": 1
            }
        """.trimIndent()

        val decoded = StateCodec.decode(legacy)
        assertNotNull(decoded, "state.json da 1.3.0 não pode quebrar o boot")
        assertEquals("cron", decoded.selectedId)
        assertEquals(listOf("base64"), decoded.favorites)

        val state = AppState().mergedWith(decoded)
        assertEquals("cron", state.selectedId)
        assertEquals(ThemeMode.Light, state.theme)
        assertEquals(AccentColor.Teal, state.accent)
    }

    @Test
    fun changesArePersistedAfterTheDebounce() = runTest {
        val store = InMemoryStateStore()
        val vm = AppViewModel(backgroundScope, AppState(), store)

        vm.select("cron")
        vm.toggleFavorite("cron")
        vm.toggleTheme()
        advanceTimeBy(PERSIST_DEBOUNCE_MS + 50)

        val saved = store.load()!!
        assertEquals("cron", saved.selectedId)
        assertTrue("cron" in saved.favorites)
        assertEquals("light", saved.theme)
    }

    @Test
    fun typingDoesNotTriggerAWriteBecauseInputsAreNotPersisted() = runTest {
        val store = InMemoryStateStore()
        val vm = AppViewModel(backgroundScope, AppState(), store)
        advanceTimeBy(PERSIST_DEBOUNCE_MS + 50)
        val before = store.saveCount

        vm.updateInput("base64", dev.devtoolbox.core.ToolInput.Text("segredo"))
        advanceTimeBy(PERSIST_DEBOUNCE_MS + 50)

        assertEquals(before, store.saveCount, "entrada do usuário não deveria ir para disco")
    }

    @Test
    fun accentChoiceSurvivesARoundTripThroughTheStore() = runTest {
        val store = InMemoryStateStore()
        val vm = AppViewModel(backgroundScope, AppState(), store)

        vm.selectAccent(AccentColor.Amber)
        advanceTimeBy(PERSIST_DEBOUNCE_MS + 50)
        assertEquals("amber", store.load()?.accent)

        assertEquals(
            AccentColor.Amber,
            AppViewModel(backgroundScope, AppState(), store).state.value.accent,
        )
    }

    @Test
    fun anUnknownAccentIdFallsBackToTheDefault() {
        val state = AppState().mergedWith(PersistedState(accent = "fucsia-neon"))
        assertEquals(AccentColor.default, state.accent)
    }

    @Test
    fun viewModelStartsFromTheStoredState() = runTest {
        val store = InMemoryStateStore(PersistedState(selectedId = "qr", theme = "light"))
        val vm = AppViewModel(backgroundScope, AppState(), store)
        assertEquals("qr", vm.state.value.selectedId)
        assertEquals(ThemeMode.Light, vm.state.value.theme)
    }
}

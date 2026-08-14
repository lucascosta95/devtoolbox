package dev.devtoolbox.ui

import dev.devtoolbox.core.BuildInfo
import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.ToolRegistry
import dev.devtoolbox.core.persistence.NoOpStateStore
import dev.devtoolbox.core.persistence.PersistedState
import dev.devtoolbox.core.persistence.StateStore
import dev.devtoolbox.core.update.GitHubReleases
import dev.devtoolbox.core.update.NoOpReleaseFetcher
import dev.devtoolbox.core.update.Release
import dev.devtoolbox.core.update.ReleaseFetcher
import dev.devtoolbox.core.update.pendingUpdate
import dev.devtoolbox.ds.AccentColor
import dev.devtoolbox.ds.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val INPUT_DEBOUNCE_MS = 150L

const val PERSIST_DEBOUNCE_MS = 500L

sealed interface UpdateNotice {
    data class Available(val release: Release) : UpdateNotice
    data object UpToDate : UpdateNotice
}

data class AppState(
    val query: String = "",
    val selectedId: String = ToolRegistry.default.id,
    val favorites: Set<String> = setOf("base64", "json"),
    val theme: ThemeMode = ThemeMode.Dark,
    val accent: AccentColor = AccentColor.default,
    val inputs: Map<String, ToolInput> = emptyMap(),
    val skippedVersion: String? = null,
    val updateNotice: UpdateNotice? = null,
) {
    val selectedTool: Tool get() = ToolRegistry.byId(selectedId) ?: ToolRegistry.default

    val currentInput: ToolInput get() = inputs[selectedId] ?: selectedTool.defaultInput

    val isSearching: Boolean get() = query.isNotBlank()

    val categories: List<Pair<Category, List<Tool>>> get() = ToolRegistry.categorized(query)

    val favoriteTools: List<Tool>
        get() = if (isSearching) emptyList() else ToolRegistry.all.filter { it.id in favorites }

    val hasResults: Boolean get() = categories.isNotEmpty()

    val navigableTools: List<Tool>
        get() = (favoriteTools + categories.flatMap { it.second }).distinct()
}

class AppViewModel(
    private val scope: CoroutineScope,
    initialState: AppState = AppState(),
    private val store: StateStore = NoOpStateStore,
    private val releases: ReleaseFetcher = NoOpReleaseFetcher,
    private val openUrl: (String) -> Unit = ::openInBrowser,
) {

    private val _state = MutableStateFlow(store.load()?.let(initialState::mergedWith) ?: initialState)
    val state: StateFlow<AppState> = _state.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val output: StateFlow<ToolOutput> = _state
        .map { it.selectedId to it.currentInput }
        .distinctUntilChanged()
        .debounce { (id, _) -> if (id == lastComputedId) INPUT_DEBOUNCE_MS else 0L }
        .mapLatest { (id, input) ->
            lastComputedId = id
            val tool = ToolRegistry.byId(id) ?: ToolRegistry.default
            tool.run(input)
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = _state.value.selectedTool.run(_state.value.currentInput),
        )

    private var lastComputedId: String = _state.value.selectedId

    init {
        @OptIn(FlowPreview::class)
        _state
            .map { it.persisted() }
            .distinctUntilChanged()
            .debounce(PERSIST_DEBOUNCE_MS)
            .onEach(store::save)
            .launchIn(scope)
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun clearQuery() = _state.update { it.copy(query = "") }

    fun select(id: String) = _state.update { it.copy(selectedId = id) }

    fun move(delta: Int) {
        val tools = _state.value.navigableTools
        if (tools.isEmpty()) return
        val current = tools.indexOfFirst { it.id == _state.value.selectedId }
        val next = when {
            current < 0 -> if (delta > 0) 0 else tools.lastIndex
            else -> (current + delta).coerceIn(0, tools.lastIndex)
        }
        select(tools[next].id)
    }

    fun toggleFavorite(id: String) = _state.update { current ->
        current.copy(
            favorites = if (id in current.favorites) current.favorites - id else current.favorites + id,
        )
    }

    fun toggleFavoriteOfSelected() = toggleFavorite(_state.value.selectedId)

    fun toggleTheme() = _state.update { it.copy(theme = it.theme.toggled()) }

    fun selectAccent(accent: AccentColor) = _state.update { it.copy(accent = accent) }

    fun checkForUpdates() {
        scope.launch { showUpdate(latestRelease(), silentWhenUpToDate = true) }
    }

    fun checkForUpdatesManually() {
        _state.update { it.copy(skippedVersion = null) }
        scope.launch { showUpdate(latestRelease(), silentWhenUpToDate = false) }
    }

    private suspend fun latestRelease(): Release? = try {
        releases.latest()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        null
    }

    private fun showUpdate(release: Release?, silentWhenUpToDate: Boolean) = _state.update { current ->
        val update = pendingUpdate(BuildInfo.VERSION, release, current.skippedVersion)
        current.copy(
            updateNotice = when {
                update != null -> UpdateNotice.Available(update)
                silentWhenUpToDate -> current.updateNotice
                else -> UpdateNotice.UpToDate
            },
        )
    }

    fun openRelease(release: Release) {
        openUrl(release.url.ifBlank { GitHubReleases.latestPageUrl(BuildInfo.REPO) })
        dismissUpdate()
    }

    fun dismissUpdate() = _state.update { it.copy(updateNotice = null) }

    fun skipVersion(release: Release) = _state.update {
        it.copy(updateNotice = null, skippedVersion = release.version?.toString() ?: release.tag)
    }

    fun updateInput(id: String, input: ToolInput) = _state.update {
        it.copy(inputs = it.inputs + (id to input))
    }

    fun resetInput(id: String) = _state.update { it.copy(inputs = it.inputs - id) }
}

fun AppState.persisted() = PersistedState(
    selectedId = selectedId,
    favorites = favorites.toList().sorted(),
    theme = if (theme == ThemeMode.Dark) "dark" else "light",
    accent = accent.id,
    skippedVersion = skippedVersion,
)

fun AppState.mergedWith(saved: PersistedState): AppState = copy(
    selectedId = saved.selectedId?.takeIf { ToolRegistry.byId(it) != null } ?: selectedId,
    favorites = saved.favorites.filter { ToolRegistry.byId(it) != null }.toSet(),
    theme = when (saved.theme) {
        "light" -> ThemeMode.Light
        "dark" -> ThemeMode.Dark
        else -> theme
    },
    accent = AccentColor.byId(saved.accent) ?: accent,
    skippedVersion = saved.skippedVersion,
)

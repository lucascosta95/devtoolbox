package dev.devtoolbox.ui

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.ToolRegistry
import dev.devtoolbox.core.persistence.NoOpStateStore
import dev.devtoolbox.core.persistence.PersistedState
import dev.devtoolbox.core.persistence.StateStore
import dev.devtoolbox.ds.ThemeMode
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

/** Intervalo de espera antes de reprocessar a entrada, como pede o handoff. */
const val INPUT_DEBOUNCE_MS = 150L

/** Espera antes de gravar o estado em disco — evita I/O a cada tecla. */
const val PERSIST_DEBOUNCE_MS = 500L

/** Máximo de ferramentas na seção Recentes. */
const val MAX_RECENT = 5

data class AppState(
    val query: String = "",
    val selectedId: String = ToolRegistry.default.id,
    val favorites: Set<String> = setOf("base64", "json"),
    val recent: List<String> = emptyList(),
    val theme: ThemeMode = ThemeMode.Dark,
    val inputs: Map<String, ToolInput> = emptyMap(),
) {
    val selectedTool: Tool get() = ToolRegistry.byId(selectedId) ?: ToolRegistry.default

    /** Entrada corrente da ferramenta ativa — cai no `defaultInput` enquanto não foi editada. */
    val currentInput: ToolInput get() = inputs[selectedId] ?: selectedTool.defaultInput

    val isSearching: Boolean get() = query.isNotBlank()

    /** Categorias filtradas pela busca, sem as vazias. */
    val categories: List<Pair<Category, List<Tool>>> get() = ToolRegistry.categorized(query)

    /** Favoritos e recentes só aparecem com a busca vazia — regra do protótipo. */
    val favoriteTools: List<Tool>
        get() = if (isSearching) emptyList() else ToolRegistry.all.filter { it.id in favorites }

    val recentTools: List<Tool>
        get() = if (isSearching) emptyList() else recent.mapNotNull { ToolRegistry.byId(it) }

    val hasResults: Boolean get() = categories.isNotEmpty()

    /** Ordem que as setas percorrem: o resultado da busca, na ordem das categorias. */
    val navigableTools: List<Tool> get() = categories.flatMap { it.second }
}

/**
 * State holder único da aplicação.
 *
 * Fica fora de qualquer composable de propósito: a UI só observa [state] e [output].
 */
class AppViewModel(
    private val scope: CoroutineScope,
    initialState: AppState = AppState(),
    private val store: StateStore = NoOpStateStore,
) {

    private val _state = MutableStateFlow(store.load()?.let(initialState::mergedWith) ?: initialState)
    val state: StateFlow<AppState> = _state.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val output: StateFlow<ToolOutput> = _state
        .map { it.selectedId to it.currentInput }
        .distinctUntilChanged()
        // Trocar de ferramenta responde na hora; digitar espera o debounce.
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

    fun select(id: String) = _state.update { current ->
        current.copy(
            selectedId = id,
            recent = (listOf(id) + current.recent.filterNot { it == id }).take(MAX_RECENT),
        )
    }

    /** Move a seleção [delta] posições na lista visível — usado pelas setas do teclado. */
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

    fun updateInput(id: String, input: ToolInput) = _state.update {
        it.copy(inputs = it.inputs + (id to input))
    }

    /** Restaura a entrada de exemplo da ferramenta ativa. */
    fun resetInput(id: String) = _state.update { it.copy(inputs = it.inputs - id) }
}

/** Recorte do estado que vai para disco. */
fun AppState.persisted() = PersistedState(
    selectedId = selectedId,
    favorites = favorites.toList().sorted(),
    recent = recent,
    theme = if (theme == ThemeMode.Dark) "dark" else "light",
)

/**
 * Aplica o estado salvo sobre o inicial, ignorando ids de ferramentas que não existem mais
 * (o catálogo pode encolher entre versões).
 */
fun AppState.mergedWith(saved: PersistedState): AppState = copy(
    selectedId = saved.selectedId?.takeIf { ToolRegistry.byId(it) != null } ?: selectedId,
    favorites = saved.favorites.filter { ToolRegistry.byId(it) != null }.toSet(),
    recent = saved.recent.filter { ToolRegistry.byId(it) != null }.take(MAX_RECENT),
    theme = when (saved.theme) {
        "light" -> ThemeMode.Light
        "dark" -> ThemeMode.Dark
        else -> theme
    },
)

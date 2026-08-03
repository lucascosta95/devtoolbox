package dev.devtoolbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.persistence.NoOpStateStore
import dev.devtoolbox.core.persistence.StateStore
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.NocturneTheme
import dev.devtoolbox.ds.ThemeMode
import dev.devtoolbox.ds.components.IconButton
import dev.devtoolbox.ds.components.PhosphorIcon
import dev.devtoolbox.ds.components.Text

/** Largura máxima do conteúdo, alinhado à esquerda (760 px no protótipo). */
private val CONTENT_MAX_WIDTH = 760.dp

@Composable
fun App(
    initialState: AppState = AppState(),
    store: StateStore = NoOpStateStore,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember { AppViewModel(scope, initialState, store) }
    val state by viewModel.state.collectAsState()
    val output by viewModel.output.collectAsState()
    val searchFocus = remember { FocusRequester() }
    val rootFocus = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

    NocturneTheme(state.theme) {
        Column(
            Modifier
                .fillMaxSize()
                .focusRequester(rootFocus)
                .focusTarget()
                .onPreviewKeyEvent { event ->
                    handleShortcut(
                        event = event,
                        searchFocused = searchFocused,
                        onFocusSearch = { searchFocus.requestFocus() },
                        onClearSearch = {
                            viewModel.clearQuery()
                            rootFocus.requestFocus()
                        },
                        onMove = viewModel::move,
                        onToggleFavorite = viewModel::toggleFavoriteOfSelected,
                        onToggleTheme = viewModel::toggleTheme,
                    )
                },
        ) {
            TitleBar(state.theme, onToggleTheme = viewModel::toggleTheme)

            Row(Modifier.fillMaxSize()) {
                Sidebar(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onSelect = viewModel::select,
                    onToggleFavorite = viewModel::toggleFavorite,
                    searchFocusRequester = searchFocus,
                    onSearchFocusChange = { searchFocused = it },
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 40.dp, vertical = 22.4.dp),
                ) {
                    ToolPane(
                        tool = state.selectedTool,
                        output = output,
                        currentInput = state.currentInput,
                        favorite = state.selectedId in state.favorites,
                        onToggleFavorite = { viewModel.toggleFavorite(state.selectedId) },
                        onInputChange = { viewModel.updateInput(state.selectedId, it) },
                        modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH),
                    )
                }
            }
        }
    }
}

/**
 * Barra de título de 44 px.
 *
 * Só o miolo: os controles de janela ficam com a decoração nativa da plataforma —
 * a barra custom do protótipo era ilustração.
 */
@Composable
private fun TitleBar(theme: ThemeMode, onToggleTheme: () -> Unit) {
    val colors = Nocturne.colors
    Box(
        Modifier.fillMaxWidth().height(44.dp).background(colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
        ) {
            PhosphorIcon("wrench", size = 14.dp, tint = colors.text(0.65f))
            Text("DevToolbox", style = Nocturne.type.item, color = colors.text(0.65f))
        }

        IconButton(
            icon = if (theme == ThemeMode.Dark) "sun" else "moon",
            contentDescription = if (theme == ThemeMode.Dark) "Usar tema claro" else "Usar tema escuro",
            onClick = onToggleTheme,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = Nocturne.space.sm),
        )

        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .background(colors.divider)
                .align(Alignment.BottomCenter),
        )
    }
}

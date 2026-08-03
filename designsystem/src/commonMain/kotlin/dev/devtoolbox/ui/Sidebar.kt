package dev.devtoolbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.Tool
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.SectionHeader
import dev.devtoolbox.ds.components.SidebarItem
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ds.components.TextField

/** Largura fixa da sidebar (280 px no protótipo). */
private val SIDEBAR_WIDTH = 280.dp

@Composable
fun Sidebar(
    state: AppState,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    onSearchFocusChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier.width(SIDEBAR_WIDTH).fillMaxHeight().background(Nocturne.colors.surface)) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.padding(11.2.dp)) {
                TextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = "Buscar ferramenta…",
                    leadingIcon = "magnifying-glass",
                    focusRequester = searchFocusRequester,
                    onFocusChange = onSearchFocusChange,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Nocturne.space.xs, vertical = Nocturne.space.xxs),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                if (state.favoriteTools.isNotEmpty()) {
                    SectionHeader("Favoritos", icon = "star", iconFill = true)
                    ToolList(state.favoriteTools, state, onSelect, onToggleFavorite)
                }

                if (state.recentTools.isNotEmpty()) {
                    SectionHeader("Recentes", icon = "clock-counter-clockwise")
                    ToolList(state.recentTools, state, onSelect, onToggleFavorite)
                }

                for ((category, tools) in state.categories) {
                    SectionHeader(category.label, icon = category.icon)
                    ToolList(tools, state, onSelect, onToggleFavorite)
                }

                if (!state.hasResults) {
                    Box(
                        Modifier.fillMaxWidth().padding(Nocturne.space.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Nenhuma ferramenta encontrada para \"${state.query}\"",
                            style = Nocturne.type.mono.copy(textAlign = TextAlign.Center),
                            color = Nocturne.colors.text(0.5f),
                        )
                    }
                }
            }
        }

        // Borda direita de 1 px, como no protótipo.
        Box(
            Modifier.fillMaxHeight().width(1.dp)
                .background(Nocturne.colors.divider)
                .align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun ToolList(
    tools: List<Tool>,
    state: AppState,
    onSelect: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    for (tool in tools) {
        SidebarItem(
            label = tool.name,
            icon = tool.icon,
            selected = tool.id == state.selectedId,
            favorite = tool.id in state.favorites,
            onSelect = { onSelect(tool.id) },
            onToggleFavorite = { onToggleFavorite(tool.id) },
        )
    }
}

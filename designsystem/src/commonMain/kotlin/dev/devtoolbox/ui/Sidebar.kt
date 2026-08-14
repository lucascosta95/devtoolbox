package dev.devtoolbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.BuildInfo
import dev.devtoolbox.core.Tool
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.OutlinedButton
import dev.devtoolbox.ds.components.PhosphorIcon
import dev.devtoolbox.ds.components.SectionHeader
import dev.devtoolbox.ds.components.SidebarItem
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ds.components.TextField

private val SIDEBAR_WIDTH = 280.dp

private val FOOTER_HEIGHT = 44.dp

@Composable
fun Sidebar(
    state: AppState,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    onSearchFocusChange: (Boolean) -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
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

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val listMinHeight = (maxHeight - FOOTER_HEIGHT).coerceAtLeast(0.dp)

                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = listMinHeight)
                            .padding(horizontal = Nocturne.space.xs, vertical = Nocturne.space.xxs),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        if (state.favoriteTools.isNotEmpty()) {
                            SectionHeader("Favoritos", icon = "star", iconFill = true)
                            ToolList(state.favoriteTools, state, onSelect, onToggleFavorite)
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

                    SidebarFooter(onCheckForUpdates)
                }
            }
        }

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

@Composable
private fun SidebarFooter(onCheckForUpdates: () -> Unit) {
    val colors = Nocturne.colors
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Nocturne.space.lg,
                    end = Nocturne.space.lg - Nocturne.space.xs,
                    top = Nocturne.space.md,
                    bottom = Nocturne.space.lg,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
            ) {
                PhosphorIcon("wrench", size = 12.dp, tint = colors.text(0.45f))
                Text("DevToolbox", style = Nocturne.type.tag, color = colors.text(0.45f))
            }
            OutlinedButton(
                label = BuildInfo.displayVersion,
                onClick = onCheckForUpdates,
                modifier = Modifier.semantics { contentDescription = "Procurar atualizações" },
                bordered = false,
                accentBorder = false,
                contentColor = colors.text(0.45f),
                textStyle = Nocturne.type.tag,
                contentPadding = PaddingValues(horizontal = Nocturne.space.xs, vertical = 4.dp),
            )
        }
    }
}

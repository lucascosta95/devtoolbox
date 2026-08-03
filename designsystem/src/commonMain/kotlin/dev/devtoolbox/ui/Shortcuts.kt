package dev.devtoolbox.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Atalhos da janela.
 *
 * `Ctrl` e `Cmd` são tratados como o mesmo modificador para o mesmo atalho funcionar em
 * macOS, Linux e Windows sem ramificação por plataforma.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun handleShortcut(
    event: KeyEvent,
    searchFocused: Boolean,
    onFocusSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onMove: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleTheme: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val command = event.isCtrlPressed || event.isMetaPressed

    return when {
        // Ctrl/Cmd+K e Ctrl/Cmd+F focam a busca.
        command && (event.key == Key.K || event.key == Key.F) -> { onFocusSearch(); true }

        command && event.isShiftPressed && event.key == Key.L -> { onToggleTheme(); true }

        command && event.key == Key.D -> { onToggleFavorite(); true }

        event.key == Key.Escape -> { onClearSearch(); true }

        // As setas navegam a lista **enquanto a busca está com o foco** — o fluxo natural é
        // Ctrl+K, digitar, descer até a ferramenta. Fora daí elas pertencem ao editor de
        // entrada, e roubá-las atrapalharia quem está escrevendo.
        searchFocused && event.key == Key.DirectionDown -> { onMove(1); true }
        searchFocused && event.key == Key.DirectionUp -> { onMove(-1); true }

        else -> false
    }
}

package dev.devtoolbox.ds.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import dev.devtoolbox.ds.Nocturne
import kotlinx.coroutines.delay

/** Duração do feedback de "Copiado", igual ao protótipo. */
private const val COPIED_FEEDBACK_MS = 1300L

/**
 * Botão fantasma "Copiar": escreve no clipboard e vira ✓ "Copiado" por 1.3 s.
 *
 * O estado do feedback é local ao botão — não suja o [dev.devtoolbox.core] nem o AppState.
 */
@Composable
fun CopyButton(
    text: String,
    modifier: Modifier = Modifier,
    label: String = "Copiar",
    copiedLabel: String = "Copiado",
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS)
            copied = false
        }
    }

    OutlinedButton(
        label = if (copied) copiedLabel else label,
        icon = if (copied) "check" else "copy",
        // Fantasma, como no protótipo: só ganha presença enquanto mostra "Copiado".
        bordered = copied,
        accentBorder = copied,
        contentColor = if (copied) Nocturne.colors.onAccentSurface else Nocturne.colors.text(0.7f),
        onClick = {
            clipboard.setText(AnnotatedString(text))
            copied = true
        },
        modifier = modifier,
    )
}

package dev.devtoolbox.ds.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.focusRing
import kotlinx.coroutines.delay

const val COPIED_FEEDBACK_MS = 1300L

@Stable
class CopyFeedbackState {
    var copiedKey: String? by mutableStateOf(null)
        private set

    fun markCopied(key: String) {
        copiedKey = key
    }

    fun clear(key: String) {
        if (copiedKey == key) copiedKey = null
    }
}

val LocalCopyFeedback = staticCompositionLocalOf { CopyFeedbackState() }

@Composable
fun CopyButton(
    text: String,
    copyKey: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    copiedLabel: String = "Copiado",
    contentDescription: String = "Copiar",
) {
    val clipboard = LocalClipboardManager.current
    val feedback = LocalCopyFeedback.current
    val copied = feedback.copiedKey == copyKey

    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS)
            feedback.clear(copyKey)
        }
    }

    val colors = Nocturne.colors
    val contentColor = if (copied) colors.onAccentSurface else colors.text(0.7f)
    val fill = when {
        pressed -> colors.accentPressed.copy(alpha = 0.18f)
        hovered || copied -> colors.hoverTint
        else -> Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .focusRing(focused, Nocturne.radii.sm)
            .clip(RoundedCornerShape(Nocturne.radii.sm))
            .background(fill)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null) {
                clipboard.setText(AnnotatedString(text))
                feedback.markCopied(copyKey)
            }
            .semantics { this.contentDescription = if (copied) copiedLabel else contentDescription }
            .padding(horizontal = if (label == null) 6.dp else 8.dp, vertical = 3.dp),
    ) {
        PhosphorIcon(if (copied) "check" else "copy", size = 13.dp, tint = contentColor)
        if (label != null) {
            Text(
                if (copied) copiedLabel else label,
                style = Nocturne.type.mono,
                color = contentColor,
            )
        }
    }
}

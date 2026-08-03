package dev.devtoolbox.ds.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.focusRing

/**
 * Campo `.input` do DS: altura 36, raio 8, fundo surface, borda divider,
 * com ícone opcional à esquerda (14 px, left 11) e placeholder.
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: String? = null,
    singleLine: Boolean = true,
    minHeight: Dp = 36.dp,
    textStyle: TextStyle = Nocturne.type.body,
    focusRequester: FocusRequester? = null,
    onFocusChange: (Boolean) -> Unit = {},
    /** `false` = campo nu, para uso dentro de um card (sem "card dentro de card"). */
    framed: Boolean = true,
) {
    val colors = Nocturne.colors
    var focused by remember { mutableStateOf(false) }

    val selectionColors = TextSelectionColors(
        handleColor = colors.accent,
        backgroundColor = colors.accent.copy(alpha = 0.3f),
    )

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        Box(
            // Campo de uma linha centraliza; multilinha começa no topo, como um editor.
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .focusRing(focused, if (framed) Nocturne.radii.md else Nocturne.radii.sm)
                .clip(RoundedCornerShape(Nocturne.radii.md))
                .then(
                    if (framed) {
                        Modifier
                            .background(colors.surface)
                            .border(1.dp, colors.divider, RoundedCornerShape(Nocturne.radii.md))
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (leadingIcon != null) {
                PhosphorIcon(
                    leadingIcon,
                    size = 14.dp,
                    tint = colors.text(0.5f),
                    modifier = Modifier.padding(start = 11.dp),
                )
            }
            Box(
                modifier = Modifier
                    .padding(
                        start = when {
                            leadingIcon != null -> 32.dp
                            framed -> Nocturne.space.md
                            else -> 0.dp
                        },
                        end = if (framed) Nocturne.space.md else 0.dp,
                        top = Nocturne.space.xs,
                        bottom = Nocturne.space.xs,
                    ),
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = textStyle, color = colors.text(0.4f))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = textStyle.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .onFocusChanged {
                            focused = it.isFocused
                            onFocusChange(it.isFocused)
                        },
                )
            }
        }
    }
}

/** Variante multilinha para a entrada das ferramentas (mono, altura livre). */
@Composable
fun CodeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minHeight: Dp = 96.dp,
    framed: Boolean = false,
) = TextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.height(minHeight),
    placeholder = placeholder,
    singleLine = false,
    minHeight = minHeight,
    textStyle = Nocturne.type.mono,
    framed = framed,
)

package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import dev.devtoolbox.core.DiffKind
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.Text

/**
 * Arquétipo `diff`.
 *
 * O DS é mono-accent: adição usa accent, remoção usa cinza com strikethrough — **nunca**
 * verde e vermelho.
 */
@Composable
fun DiffLayout(body: ToolBody.Diff, modifier: Modifier = Modifier) {
    val colors = Nocturne.colors
    Card(modifier.fillMaxWidth()) {
        CardKicker("Diferenças")
        Column(
            Modifier
                .padding(top = Nocturne.space.xs)
                .horizontalScroll(rememberScrollState())
                // Largura da linha mais longa: as faixas de fundo ficam alinhadas entre si.
                .width(IntrinsicSize.Max),
        ) {
            for (line in body.lines) {
                val (background, color, decoration) = when (line.kind) {
                    DiffKind.Add -> Triple(colors.accentSurface, colors.onAccentSurface, null)
                    DiffKind.Del ->
                        Triple(colors.mutedSurface, colors.text(0.55f), TextDecoration.LineThrough)
                    DiffKind.Same -> Triple(Color.Transparent, colors.text(0.85f), null)
                }
                val prefix = when (line.kind) {
                    DiffKind.Add -> "+"
                    DiffKind.Del -> "−"
                    DiffKind.Same -> " "
                }
                Text(
                    "$prefix ${line.text}",
                    style = Nocturne.type.mono.copy(lineHeight = 24.sp, textDecoration = decoration),
                    color = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(background)
                        .padding(horizontal = Nocturne.space.xs),
                )
            }
        }
    }
}

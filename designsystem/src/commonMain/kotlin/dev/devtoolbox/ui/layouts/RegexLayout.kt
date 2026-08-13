package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.PhosphorIcon
import dev.devtoolbox.ds.components.Tag
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ui.panels.COPY_ICON_RESERVE
import dev.devtoolbox.ui.panels.InputPanel
import dev.devtoolbox.ui.panels.LocalPanelStore
import dev.devtoolbox.ui.panels.PanelEdges
import dev.devtoolbox.ui.panels.ResizableBlock
import dev.devtoolbox.ui.panels.SegmentHighlight

private val SUBJECT_HEIGHT = 120.dp

private val PATTERN_MIN_WIDTH = 72.dp

private val FLAGS_MIN_WIDTH = 40.dp

private val CARET_ROOM = 2.dp

@Composable
fun RegexLayout(
    body: ToolBody.Regex,
    toolId: String,
    input: ToolInput.Pattern,
    error: String?,
    onInputChange: (ToolInput.Pattern) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Nocturne.colors
    val store = LocalPanelStore.current
    val block = store.block("$toolId-subject", SUBJECT_HEIGHT)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
        Card(Modifier.fillMaxWidth()) {
            CardKicker(
                "Padrão",
                trailingReserve = COPY_ICON_RESERVE,
                trailing = {
                    CopyButton(
                        "/${input.pattern}/${input.flags}",
                        "$toolId-pattern",
                        contentDescription = "Copiar padrão",
                    )
                },
            )
            Row(
                modifier = Modifier.padding(top = Nocturne.space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Slash()
                InlineField(
                    value = input.pattern,
                    onValueChange = { onInputChange(input.copy(pattern = it)) },
                    placeholder = "padrão",
                    minWidth = PATTERN_MIN_WIDTH,
                )
                Slash()
                InlineField(
                    value = input.flags,
                    onValueChange = { onInputChange(input.copy(flags = it)) },
                    placeholder = "flags",
                    minWidth = FLAGS_MIN_WIDTH,
                )
            }
        }

        if (error != null) {
            RegexError(error)
        }

        ResizableBlock(block) {
            InputPanel(
                kicker = "String de teste",
                value = input.subject,
                onValueChange = { onInputChange(input.copy(subject = it)) },
                block = block,
                slot = "subject",
                modifier = Modifier.fillMaxWidth(),
                placeholder = "string de teste",
                edges = PanelEdges(block = block, column = store.content),
                visualTransformation = SegmentHighlight(
                    segments = body.segments,
                    matched = colors.onAccentSurface,
                    highlight = colors.accentSurface,
                ),
            )
        }

        Card(Modifier.fillMaxWidth()) {
            CardKicker("Correspondências · ${body.matches.size}")
            if (body.matches.isEmpty()) {
                Box(Modifier.padding(top = Nocturne.space.xs)) {
                    Text(
                        "Nenhuma correspondência.",
                        style = Nocturne.type.mono,
                        color = colors.text(0.5f),
                    )
                }
            } else {
                Column(
                    Modifier.padding(top = Nocturne.space.xs),
                    verticalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
                ) {
                    for ((index, match) in body.matches.withIndex()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
                        ) {
                            Tag("${match.index}", accent = true)
                            Text(
                                match.value,
                                style = Nocturne.type.mono,
                                modifier = Modifier.weight(1f),
                            )
                            CopyButton(
                                match.value,
                                "$toolId-match$index",
                                contentDescription = "Copiar correspondência ${match.index}",
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun Slash() {
    Text("/", style = Nocturne.type.mono, color = Nocturne.colors.text(0.45f))
}

@Composable
private fun InlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minWidth: Dp,
) {
    val colors = Nocturne.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = Nocturne.type.mono

    val width = remember(value, style, density) {
        val measured = measurer.measure(AnnotatedString(value), style).size.width
        maxOf(with(density) { measured.toDp() } + CARET_ROOM, minWidth)
    }

    Box(contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) {
            Text(placeholder, style = style, color = colors.text(0.4f))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = style.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.width(width),
        )
    }
}

@Composable
private fun RegexError(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Nocturne.radii.sm))
            .background(Nocturne.colors.mutedSurface)
            .padding(horizontal = Nocturne.space.md, vertical = Nocturne.space.xs),
    ) {
        PhosphorIcon("x-circle", size = 14.dp, fill = true, tint = Nocturne.colors.text(0.6f))
        Text(message, style = Nocturne.type.mono, color = Nocturne.colors.text(0.75f))
    }
}

package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.Tag
import dev.devtoolbox.ds.components.Text

/** Arquétipo `regex`: o padrão, a string de teste com os trechos casados e a lista de matches. */
@Composable
fun RegexLayout(body: ToolBody.Regex, toolId: String, modifier: Modifier = Modifier) {
    val colors = Nocturne.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
        Card {
            val pattern = "/${body.pattern}/${body.flags}"
            CardKicker("Padrão") {
                CopyButton(pattern, "$toolId-pattern", contentDescription = "Copiar padrão")
            }
            Box(Modifier.padding(top = Nocturne.space.xs)) {
                Text(
                    pattern,
                    style = Nocturne.type.body,
                    color = colors.onAccentSurface,
                )
            }
        }

        Card {
            CardKicker("String de teste")
            Box(Modifier.padding(top = Nocturne.space.xs)) {
                Text(
                    buildAnnotatedString {
                        for (segment in body.segments) {
                            if (segment.matched) {
                                withStyle(
                                    SpanStyle(
                                        color = colors.onAccentSurface,
                                        background = colors.accentSurface,
                                    ),
                                ) { append(segment.text) }
                            } else {
                                withStyle(SpanStyle(color = colors.text(0.85f))) { append(segment.text) }
                            }
                        }
                    },
                    style = Nocturne.type.mono,
                )
            }
        }

        Card {
            CardKicker("Correspondências (${body.matches.size})")
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

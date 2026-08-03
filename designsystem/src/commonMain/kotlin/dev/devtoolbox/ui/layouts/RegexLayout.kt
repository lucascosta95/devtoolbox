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
import dev.devtoolbox.ds.components.Tag
import dev.devtoolbox.ds.components.Text

/** Arquétipo `regex`: o padrão, a string de teste com os trechos casados e a lista de matches. */
@Composable
fun RegexLayout(body: ToolBody.Regex, modifier: Modifier = Modifier) {
    val colors = Nocturne.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
        Card {
            CardKicker("Padrão")
            Box(Modifier.padding(top = Nocturne.space.xs)) {
                Text(
                    "/${body.pattern}/${body.flags}",
                    style = Nocturne.type.body.copy(fontFamily = Nocturne.type.mono.fontFamily),
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
                    for (match in body.matches) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
                        ) {
                            Tag("${match.index}", accent = true)
                            Text(match.value, style = Nocturne.type.mono)
                        }
                    }
                }
            }
        }
    }
}

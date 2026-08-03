package dev.devtoolbox.ds.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import dev.devtoolbox.ds.Nocturne

/**
 * `Text` do DS sobre `BasicText` — o projeto não usa Material, então esta é a única
 * porta de entrada para texto e todo estilo vem de [Nocturne.type].
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Nocturne.type.body,
    color: Color = Nocturne.colors.text,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) = BasicText(
    text = text,
    modifier = modifier,
    style = style.copy(color = color),
    maxLines = maxLines,
    overflow = overflow,
)

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = Nocturne.type.body,
    color: Color = Nocturne.colors.text,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) = BasicText(
    text = text,
    modifier = modifier,
    style = style.copy(color = color),
    maxLines = maxLines,
    overflow = overflow,
)

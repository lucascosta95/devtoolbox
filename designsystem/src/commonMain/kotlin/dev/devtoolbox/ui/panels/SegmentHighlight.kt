package dev.devtoolbox.ui.panels

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.devtoolbox.core.Segment

class SegmentHighlight(
    private val segments: List<Segment>,
    private val matched: Color,
    private val highlight: Color,
    private val dimmed: Color? = null,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (segments.joinToString("") { it.text } != raw) {
            return TransformedText(AnnotatedString(raw), OffsetMapping.Identity)
        }

        val colored = buildAnnotatedString {
            append(raw)
            var cursor = 0
            for (segment in segments) {
                val end = cursor + segment.text.length
                when {
                    segment.matched ->
                        addStyle(SpanStyle(color = matched, background = highlight), cursor, end)
                    dimmed != null -> addStyle(SpanStyle(color = dimmed), cursor, end)
                }
                cursor = end
            }
        }

        return TransformedText(colored, OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean =
        other is SegmentHighlight &&
            other.segments == segments &&
            other.matched == matched &&
            other.highlight == highlight &&
            other.dimmed == dimmed

    override fun hashCode(): Int {
        var result = segments.hashCode()
        result = 31 * result + matched.hashCode()
        result = 31 * result + highlight.hashCode()
        result = 31 * result + dimmed.hashCode()
        return result
    }
}

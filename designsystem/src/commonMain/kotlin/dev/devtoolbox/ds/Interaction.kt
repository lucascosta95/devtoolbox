package dev.devtoolbox.ds

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Indicação vazia: o Nocturne não usa ripple — hover e pressed são tintas do ramp,
 * aplicadas explicitamente por cada componente.
 */
object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = NoIndicationNode()
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = -1
}

private class NoIndicationNode : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() = drawContent()
}

@Suppress("unused")
private val indicationContract: Indication = NoIndication

/**
 * Anel de foco do DS: `outline: 2px solid accent; outline-offset: 2px`.
 *
 * Desenha **fora** dos limites do componente, como o outline do CSS — por isso não usa borda.
 */
@Composable
fun Modifier.focusRing(
    focused: Boolean,
    cornerRadius: Dp = Nocturne.radii.sm,
    offset: Dp = 2.dp,
    width: Dp = 2.dp,
): Modifier {
    val accent = Nocturne.colors.accent
    return this.drawWithContent {
        drawContent()
        if (!focused) return@drawWithContent
        val inset = -(offset.toPx() + width.toPx() / 2f)
        val radius = cornerRadius.toPx() + offset.toPx()
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(
                        offset = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                ),
            )
        }
        drawPath(path, color = accent, style = Stroke(width = width.toPx()))
    }
}

/** Opacidade de elemento desabilitado no DS. */
const val DISABLED_ALPHA = 0.45f

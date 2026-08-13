package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.Ramp
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ui.panels.InputPanel
import dev.devtoolbox.ui.panels.LocalPanelStore
import dev.devtoolbox.ui.panels.OutputPanel
import dev.devtoolbox.ui.panels.PanelBlock
import dev.devtoolbox.ui.panels.PanelEdges
import dev.devtoolbox.ui.panels.ResizableBlock
import dev.devtoolbox.ui.panels.ResizableRow
import dev.devtoolbox.ui.panels.SplitSide

private val TOKEN_HEIGHT = 120.dp

private val DECODED_HEIGHT = 200.dp

@Composable
fun JwtLayout(
    body: ToolBody.Jwt,
    toolId: String,
    token: String,
    error: String?,
    onTokenChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Nocturne.colors
    val store = LocalPanelStore.current
    val tokenBlock = store.block("$toolId-token", TOKEN_HEIGHT)
    val decodedBlock = store.block("$toolId-decoded", DECODED_HEIGHT)

    val transformation = remember(colors.onAccentSurface, colors.text) {
        JwtTokenTransformation(
            header = colors.onAccentSurface,
            payload = Ramp.neutral300,
            signature = colors.text(0.45f),
        )
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
        ResizableBlock(tokenBlock) {
            InputPanel(
                kicker = "Token",
                value = token,
                onValueChange = onTokenChange,
                block = tokenBlock,
                slot = "token",
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Cole o token aqui…",
                visualTransformation = transformation,
                edges = PanelEdges(block = tokenBlock),
                trailing = {
                    CopyButton(token, "$toolId-token", contentDescription = "Copiar token")
                },
            )
        }

        ResizableBlock(decodedBlock) {
            ResizableRow(
                block = decodedBlock,
                left = {
                    DecodedPanel(
                        kicker = "Header",
                        json = body.headerJson,
                        error = error,
                        block = decodedBlock,
                        slot = "header",
                        copyKey = "$toolId-header",
                        edges = PanelEdges(block = decodedBlock, splitSide = SplitSide.End),
                    )
                },
                right = {
                    DecodedPanel(
                        kicker = "Payload",
                        json = body.payloadJson,
                        error = error,
                        block = decodedBlock,
                        slot = "payload",
                        copyKey = "$toolId-payload",
                        edges = PanelEdges(
                            block = decodedBlock,
                            splitSide = SplitSide.Start,
                            column = store.content,
                        ),
                    )
                },
            )
        }

        Text(
            "A assinatura não é verificada — esta ferramenta só decodifica o token.",
            style = Nocturne.type.label,
            color = colors.text(0.5f),
        )
    }
}

@Composable
private fun DecodedPanel(
    kicker: String,
    json: String,
    error: String?,
    block: PanelBlock,
    slot: String,
    copyKey: String,
    edges: PanelEdges,
) {
    val failed = error != null && json.isBlank()
    OutputPanel(
        kicker = kicker,
        text = AnnotatedString(if (failed) error else json),
        block = block,
        slot = slot,
        modifier = Modifier.fillMaxWidth(),
        color = if (failed) Nocturne.colors.text(0.6f) else Nocturne.colors.text(0.9f),
        edges = edges,
        trailing = if (failed) {
            null
        } else {
            { CopyButton(json, copyKey, contentDescription = "Copiar $kicker") }
        },
    )
}

private class JwtTokenTransformation(
    private val header: Color,
    private val payload: Color,
    private val signature: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val first = raw.indexOf('.')
        val second = if (first < 0) -1 else raw.indexOf('.', first + 1)

        val colored = buildAnnotatedString {
            append(raw)
            when {
                first < 0 -> addStyle(SpanStyle(color = header), 0, raw.length)
                second < 0 -> {
                    addStyle(SpanStyle(color = header), 0, first)
                    addStyle(SpanStyle(color = payload), first + 1, raw.length)
                }
                else -> {
                    addStyle(SpanStyle(color = header), 0, first)
                    addStyle(SpanStyle(color = payload), first + 1, second)
                    addStyle(SpanStyle(color = signature), second + 1, raw.length)
                }
            }
        }

        return TransformedText(colored, OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean =
        other is JwtTokenTransformation &&
            other.header == header &&
            other.payload == payload &&
            other.signature == signature

    override fun hashCode(): Int =
        header.hashCode() * 31 * 31 + payload.hashCode() * 31 + signature.hashCode()
}

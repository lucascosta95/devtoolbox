package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.Ramp
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.Text

/**
 * Arquétipo `jwt`: o token colorido por parte, e header/payload decodificados em dois cards.
 *
 * As cores das partes vêm do handoff: header em accent-300, payload em neutral-300 e a
 * assinatura no texto a 45%.
 */
@Composable
fun JwtLayout(body: ToolBody.Jwt, toolId: String, modifier: Modifier = Modifier) {
    val colors = Nocturne.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
        Card {
            CardKicker("Token") {
                CopyButton(token(body), "$toolId-token", contentDescription = "Copiar token")
            }
            Box(Modifier.padding(top = Nocturne.space.xs)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.onAccentSurface)) { append(body.headerPart) }
                        withStyle(SpanStyle(color = colors.text(0.45f))) { append(".") }
                        withStyle(SpanStyle(color = Ramp.neutral300)) { append(body.payloadPart) }
                        withStyle(SpanStyle(color = colors.text(0.45f))) { append(".") }
                        withStyle(SpanStyle(color = colors.text(0.45f))) { append(body.signaturePart) }
                    },
                    style = Nocturne.type.mono,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
        ) {
            DecodedCard("Header", body.headerJson, "$toolId-header", Modifier.weight(1f))
            DecodedCard("Payload", body.payloadJson, "$toolId-payload", Modifier.weight(1f))
        }

        Text(
            "A assinatura não é verificada — esta ferramenta só decodifica o token.",
            style = Nocturne.type.label,
            color = colors.text(0.5f),
        )
    }
}

@Composable
private fun DecodedCard(
    kicker: String,
    json: String,
    copyKey: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxHeight()) {
        CardKicker(kicker) { CopyButton(json, copyKey, contentDescription = "Copiar $kicker") }
        Box(Modifier.padding(top = Nocturne.space.xs)) {
            Text(json, style = Nocturne.type.mono, color = Nocturne.colors.text(0.9f))
        }
    }
}

private fun token(body: ToolBody.Jwt): String =
    "${body.headerPart}.${body.payloadPart}.${body.signaturePart}"

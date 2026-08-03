package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.ImageSelection
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.Divider
import dev.devtoolbox.ds.components.LabelValueRow
import dev.devtoolbox.ds.components.PhosphorIcon
import dev.devtoolbox.ds.components.Tag
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ui.PickedFile
import dev.devtoolbox.ui.imageDropTarget
import dev.devtoolbox.ui.loadImageSelection
import dev.devtoolbox.ui.pickImageFile
import kotlinx.coroutines.launch

/** Largura fixa da área de soltar, do handoff. */
private val DROP_ZONE_WIDTH = 168.dp

/** A coluna de label deste card é mais estreita que a dos outros — o card é curto. */
private val FILE_LABEL_WIDTH = 108.dp

/** A data URI é longa por natureza: mostra ~120 dp e o resto vai no botão de copiar. */
private val DATA_URI_MAX_HEIGHT = 120.dp

/**
 * Arquétipo `image`: área de soltar + dados do arquivo, a data URI e os snippets.
 *
 * A leitura e a codificação são disparadas daqui, mas acontecem fora da thread de UI
 * (ver [loadImageSelection]); enquanto rodam, o estado vira [ImageSelection.Loading].
 */
@Composable
fun ImageLayout(
    body: ToolBody.Image,
    toolId: String,
    onSelectionChange: (ImageSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    fun load(file: PickedFile) {
        scope.launch {
            onSelectionChange(ImageSelection.Loading(file.name))
            onSelectionChange(loadImageSelection(file))
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
        ) {
            DropZone(
                onPick = { scope.launch { pickImageFile()?.let(::load) } },
                onFileDropped = ::load,
            )

            val loaded = body.details
            Card(Modifier.weight(1f).fillMaxHeight()) {
                CardKicker("Arquivo carregado")
                when {
                    body.loading -> Placeholder("Lendo e codificando…")
                    loaded == null -> Placeholder("Nenhuma imagem carregada ainda.")
                    else -> Column(Modifier.padding(top = Nocturne.space.xs)) {
                        for ((index, row) in loaded.rows.withIndex()) {
                            LabelValueRow(
                                label = row.label,
                                value = row.value,
                                labelWidth = FILE_LABEL_WIDTH,
                            )
                            if (index != loaded.rows.lastIndex) Divider()
                        }
                    }
                }
            }
        }

        val details = body.details ?: return@Column

        Card(Modifier.fillMaxWidth()) {
            CardKicker("Data URI") {
                CopyButton(details.dataUri, "$toolId-uri", label = "Copiar")
            }
            Box(
                Modifier
                    .padding(top = Nocturne.space.xs)
                    .heightIn(max = DATA_URI_MAX_HEIGHT)
                    // Recorta em vez de rolar: quem quer a string inteira usa o botão.
                    .clip(RoundedCornerShape(Nocturne.radii.sm)),
            ) {
                Text(details.dataUri, style = Nocturne.type.mono, color = Nocturne.colors.text(0.9f))
            }
        }

        Card(Modifier.fillMaxWidth()) {
            CardKicker("Snippets")
            Column(Modifier.padding(top = Nocturne.space.xs)) {
                for ((index, snippet) in details.snippets.withIndex()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Nocturne.space.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
                    ) {
                        Tag(snippet.label)
                        Text(
                            snippet.value,
                            style = Nocturne.type.mono,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        CopyButton(
                            snippet.value,
                            "$toolId-snippet$index",
                            contentDescription = "Copiar snippet ${snippet.label}",
                        )
                    }
                    if (index != details.snippets.lastIndex) Divider()
                }
            }
        }
    }
}

@Composable
private fun DropZone(onPick: () -> Unit, onFileDropped: (PickedFile) -> Unit) {
    val colors = Nocturne.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }

    val active = hovered || dragging
    val borderColor = if (active) colors.accent else colors.text(0.28f)
    val radius = Nocturne.radii.md

    Column(
        modifier = Modifier
            .width(DROP_ZONE_WIDTH)
            .fillMaxHeight()
            .defaultMinSize(minHeight = 132.dp)
            .clip(RoundedCornerShape(radius))
            .background(if (active) colors.accentSurface.copy(alpha = 0.6f) else Color.Transparent)
            .dashedBorder(borderColor, radius)
            .imageDropTarget(
                onDragStateChange = { dragging = it },
                onFileDropped = onFileDropped,
            )
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onPick)
            .padding(Nocturne.space.md),
        verticalArrangement = Arrangement.spacedBy(Nocturne.space.xs, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PhosphorIcon("image-square", size = 28.dp, tint = colors.onAccentSurface)
        Text(
            "Arraste uma imagem\nou clique para escolher",
            style = Nocturne.type.label.copy(textAlign = TextAlign.Center),
            color = colors.text(0.7f),
        )
        Text(
            "PNG · JPG · SVG · WEBP · até 5 MB",
            style = Nocturne.type.sectionHeader.copy(
                textAlign = TextAlign.Center,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
            ),
            color = colors.text(0.45f),
        )
    }
}

@Composable
private fun Placeholder(text: String) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = Nocturne.space.md),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = Nocturne.type.mono, color = Nocturne.colors.text(0.5f))
    }
}

/** Borda tracejada de 1 px — o Compose não tem uma, então é um `drawBehind` com dash. */
private fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp) = drawBehind {
    val stroke = Stroke(
        width = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
    )
    val corner = CornerRadius(radius.toPx(), radius.toPx())
    // Meio traço para dentro: senão a linha sai pela metade fora dos limites.
    val inset = stroke.width / 2f
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = corner,
        style = stroke,
    )
}

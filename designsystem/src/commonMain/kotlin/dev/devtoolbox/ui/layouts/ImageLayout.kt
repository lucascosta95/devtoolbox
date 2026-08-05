package dev.devtoolbox.ui.layouts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.ImageSelection
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.util.EncodedImage
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.CopyButton
import dev.devtoolbox.ds.components.Divider
import dev.devtoolbox.ds.components.GhostButton
import dev.devtoolbox.ds.components.LabelValueRow
import dev.devtoolbox.ds.components.PhosphorIcon
import dev.devtoolbox.ds.components.Tag
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ui.PickedFile
import dev.devtoolbox.ui.decodeImage
import dev.devtoolbox.ui.imageDropTarget
import dev.devtoolbox.ui.ioDispatcher
import dev.devtoolbox.ui.loadImageSelection
import dev.devtoolbox.ui.pickImageFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DROP_ZONE_WIDTH = 168.dp

private val EMPTY_ZONE_MIN_HEIGHT = 148.dp
private val PREVIEW_MIN_HEIGHT = 112.dp

private val FILE_LABEL_WIDTH = 108.dp

private val DATA_URI_MAX_HEIGHT = 120.dp

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
            ImageColumn(
                source = body.source,
                onPick = { scope.launch { pickImageFile()?.let(::load) } },
                onFileDropped = ::load,
                onRemove = { onSelectionChange(ImageSelection.Empty) },
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
private fun ImageColumn(
    source: EncodedImage?,
    onPick: () -> Unit,
    onFileDropped: (PickedFile) -> Unit,
    onRemove: () -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(DROP_ZONE_WIDTH)
            .fillMaxHeight()
            .imageDropTarget(
                onDragStateChange = { dragging = it },
                onFileDropped = onFileDropped,
            ),
        verticalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
    ) {
        if (source == null) {
            EmptyDropZone(dragging = dragging, onPick = onPick)
        } else {
            ImagePreview(source = source, dragging = dragging, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GhostButton(
                    label = "Trocar",
                    icon = "arrows-clockwise",
                    onClick = onPick,
                    contentDescription = "Trocar imagem",
                    modifier = Modifier.weight(1f),
                )
                GhostButton(
                    label = "",
                    icon = "trash",
                    onClick = onRemove,
                    contentDescription = "Remover imagem",
                )
            }
        }
    }
}

@Composable
private fun EmptyDropZone(dragging: Boolean, onPick: () -> Unit) {
    val colors = Nocturne.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val active = hovered || dragging
    val radius = Nocturne.radii.md

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .defaultMinSize(minHeight = EMPTY_ZONE_MIN_HEIGHT)
            .clip(RoundedCornerShape(radius))
            .background(if (active) colors.accentSurface.copy(alpha = 0.6f) else Color.Transparent)
            .dashedBorder(if (active) colors.accent else colors.text(0.28f), radius)
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
                letterSpacing = TextUnit.Unspecified,
            ),
            color = colors.text(0.45f),
        )
    }
}

@Composable
private fun ImagePreview(source: EncodedImage, dragging: Boolean, modifier: Modifier = Modifier) {
    val colors = Nocturne.colors
    val density = LocalDensity.current
    val radius = Nocturne.radii.md

    val painter by produceState<Painter?>(initialValue = null, source) {
        value = withContext(ioDispatcher) { decodeImage(source, density) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = PREVIEW_MIN_HEIGHT)
            .clip(RoundedCornerShape(radius))
            .checkerboard(colors.surface, colors.text(0.06f))
            .border(
                width = 1.dp,
                color = if (dragging) colors.accent else colors.divider,
                shape = RoundedCornerShape(radius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val current = painter
        if (current != null) {
            Image(
                painter = current,
                contentDescription = "Pré-visualização de ${source.fileName}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(Nocturne.space.xs),
            )
        } else {
            PhosphorIcon("image-square", size = 24.dp, tint = colors.text(0.35f))
        }
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

private fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp) = drawBehind {
    val stroke = Stroke(
        width = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
    )
    val corner = CornerRadius(radius.toPx(), radius.toPx())
    val inset = stroke.width / 2f
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = corner,
        style = stroke,
    )
}

private fun Modifier.checkerboard(base: Color, tint: Color) = drawBehind {
    drawRect(base)
    val cell = 7.dp.toPx()
    var row = 0
    var y = 0f
    while (y < size.height) {
        var column = 0
        var x = 0f
        while (x < size.width) {
            if ((row + column) % 2 == 0) {
                drawRect(
                    color = tint,
                    topLeft = Offset(x, y),
                    size = Size(minOf(cell, size.width - x), minOf(cell, size.height - y)),
                )
            }
            x += cell
            column++
        }
        y += cell
        row++
    }
}

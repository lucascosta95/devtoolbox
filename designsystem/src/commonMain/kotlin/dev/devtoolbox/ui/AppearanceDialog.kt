package dev.devtoolbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.devtoolbox.ds.AccentColor
import dev.devtoolbox.ds.CustomTheme
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.Palette
import dev.devtoolbox.ds.Palettes
import dev.devtoolbox.ds.ThemeChoice
import dev.devtoolbox.ds.components.AccentPicker
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.GhostButton
import dev.devtoolbox.ds.components.ModalDialog
import dev.devtoolbox.ds.components.PhosphorIcon
import dev.devtoolbox.ds.components.SecondaryButton
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ds.components.TextField
import dev.devtoolbox.ds.customThemePresets
import dev.devtoolbox.ds.parseHexColor

private val DIALOG_WIDTH = 640.dp

private val TILE_WIDTH = 104.dp

private val PREVIEW_HEIGHT = 58.dp

@Composable
fun AppearanceDialog(
    theme: ThemeChoice,
    accent: AccentColor,
    customTheme: CustomTheme,
    onSelectTheme: (ThemeChoice) -> Unit,
    onSelectAccent: (AccentColor) -> Unit,
    onCustomThemeChange: (CustomTheme) -> Unit,
    onClose: () -> Unit,
) {
    ModalDialog(onDismiss = onClose, maxWidth = DIALOG_WIDTH) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
            ) {
                PhosphorIcon("palette", size = 13.dp, tint = Nocturne.colors.onAccentSurface)
                Column(Modifier.weight(1f)) { CardKicker("Aparência") }
            }

            SectionTitle("Tema")
            ThemeGrid(theme, accent, customTheme, onSelectTheme)

            if (theme == ThemeChoice.Custom) {
                SectionTitle("Cores do tema personalizado")
                CustomThemeEditor(customTheme, onCustomThemeChange)
            } else {
                SectionTitle("Cor de destaque")
                AccentPicker(selected = accent, onSelect = onSelectAccent)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Nocturne.space.lg),
                horizontalArrangement = Arrangement.End,
            ) {
                SecondaryButton(label = "Fechar", onClick = onClose)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = Nocturne.type.sectionHeader,
        color = Nocturne.colors.text(0.6f),
        modifier = Modifier.padding(top = Nocturne.space.lg, bottom = Nocturne.space.sm),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeGrid(
    selected: ThemeChoice,
    accent: AccentColor,
    customTheme: CustomTheme,
    onSelect: (ThemeChoice) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
        verticalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
    ) {
        for (palette in Palettes.all) {
            val choice = ThemeChoice.Preset(palette)
            ThemeTile(
                palette = palette,
                accent = accent.color,
                selected = selected == choice,
                onClick = { onSelect(choice) },
            )
        }
        ThemeTile(
            palette = customTheme.palette(),
            accent = customTheme.accentColor(),
            selected = selected == ThemeChoice.Custom,
            onClick = { onSelect(ThemeChoice.Custom) },
            icon = "drop-half",
        )
    }
}

// Uma miniatura da janela no tema: barra de título na cor de superfície, duas linhas de texto e
// um botão no destaque, para comparar os temas sem precisar aplicar cada um.
@Composable
private fun ThemeTile(
    palette: Palette,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    icon: String? = null,
) {
    val colors = Nocturne.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(Nocturne.radii.md)

    Column(
        verticalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
        modifier = Modifier
            .width(TILE_WIDTH)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = "Tema ${palette.label}"
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(PREVIEW_HEIGHT)
                .clip(shape)
                .background(palette.bg)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = when {
                        selected -> colors.accent
                        hovered -> colors.text(0.35f)
                        else -> colors.divider
                    },
                    shape = shape,
                ),
        ) {
            Box(Modifier.fillMaxWidth().height(12.dp).background(palette.surface))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            ) {
                PreviewLine(palette.text.copy(alpha = 0.85f), 46.dp)
                PreviewLine(palette.text.copy(alpha = 0.4f), 64.dp)
                Box(Modifier.width(26.dp).height(7.dp).clip(CircleShape).background(accent))
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) PhosphorIcon(icon, size = 11.dp, tint = colors.text(0.6f))
            Text(
                palette.label,
                style = Nocturne.type.tag,
                color = if (selected) colors.accent else colors.text(0.75f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PreviewLine(color: Color, width: androidx.compose.ui.unit.Dp) {
    Box(Modifier.width(width).height(4.dp).clip(CircleShape).background(color))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomThemeEditor(theme: CustomTheme, onChange: (CustomTheme) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
            HexField("Fundo", theme.bg, { onChange(theme.copy(bg = it)) }, Modifier.weight(1f))
            HexField("Superfície", theme.surface, { onChange(theme.copy(surface = it)) }, Modifier.weight(1f))
            HexField("Destaque", theme.accent, { onChange(theme.copy(accent = it)) }, Modifier.weight(1f))
        }

        Text(
            "O texto e as bordas são calculados a partir do fundo, para continuarem legíveis.",
            style = Nocturne.type.tag,
            color = Nocturne.colors.text(0.5f),
        )

        FlowRow(
            verticalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
        ) {
            for (preset in customThemePresets) {
                PresetChip(preset.label, preset.theme, onClick = { onChange(preset.theme) })
            }
            GhostButton(
                label = "Restaurar",
                icon = "arrows-clockwise",
                onClick = { onChange(CustomTheme()) },
            )
        }
    }
}

@Composable
private fun HexField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Nocturne.colors
    val parsed = parseHexColor(value)
    Column(modifier) {
        Text(label, style = Nocturne.type.tag, color = colors.text(0.6f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(Nocturne.radii.sm))
                    .background(parsed ?: Color.Transparent)
                    .border(1.dp, colors.divider, RoundedCornerShape(Nocturne.radii.sm)),
            )
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = "#rrggbb",
                textStyle = Nocturne.type.mono.copy(
                    color = if (parsed == null) colors.text(0.45f) else colors.text,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PresetChip(label: String, theme: CustomTheme, onClick: () -> Unit) {
    val colors = Nocturne.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(Nocturne.radii.md)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
        modifier = Modifier
            .clip(shape)
            .background(if (hovered) colors.hoverTint else Color.Transparent)
            .border(1.dp, colors.divider, shape)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Nocturne.space.sm, vertical = 5.dp),
    ) {
        Row {
            for (hex in listOf(theme.bg, theme.surface, theme.accent)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(hex) ?: Color.Transparent)
                        .border(1.dp, colors.divider, CircleShape),
                )
            }
        }
        Text(label, style = Nocturne.type.tag, color = colors.text(0.8f))
    }
}

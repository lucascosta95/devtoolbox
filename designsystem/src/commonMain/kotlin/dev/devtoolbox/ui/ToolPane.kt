package dev.devtoolbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.Direction
import dev.devtoolbox.core.Segment
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.IconButton
import dev.devtoolbox.ds.components.PhosphorIcon
import dev.devtoolbox.ds.components.Tag
import dev.devtoolbox.ds.components.Text
import dev.devtoolbox.ui.layouts.DiffLayout
import dev.devtoolbox.ui.layouts.ImageLayout
import dev.devtoolbox.ui.layouts.IoLayout
import dev.devtoolbox.ui.layouts.JwtLayout
import dev.devtoolbox.ui.layouts.QrLayout
import dev.devtoolbox.ui.layouts.RegexLayout
import dev.devtoolbox.ui.layouts.RowsLayout
import dev.devtoolbox.ui.layouts.ValidateLayout

@Composable
fun ToolPane(
    tool: Tool,
    output: ToolOutput,
    currentInput: ToolInput,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onInputChange: (ToolInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ToolHeader(tool, favorite, onToggleFavorite)

        val error = (output as? ToolOutput.Failure)?.message
        val body = when (output) {
            is ToolOutput.Success -> output.body
            is ToolOutput.Failure -> output.body ?: fallbackBody(tool.id, currentInput)
        }

        if (!body.hostsItsInput()) {
            ToolInputEditor(
                input = currentInput,
                onChange = onInputChange,
                singleLine = tool.singleLineInput,
                modifier = Modifier.padding(bottom = Nocturne.space.sm),
            )
        }

        if (error != null && !body.hostsItsError()) {
            ErrorMessage(error)
        }

        if (body != null) {
            ToolBodyContent(tool.id, body, currentInput, error, onInputChange)
        }
    }
}

private fun ToolBody?.hostsItsInput(): Boolean =
    this is ToolBody.Io || this is ToolBody.Image || this is ToolBody.Jwt || this is ToolBody.Regex

private fun ToolBody?.hostsItsError(): Boolean = this is ToolBody.Jwt || this is ToolBody.Regex

internal fun fallbackBody(toolId: String, input: ToolInput): ToolBody? = when {
    toolId == "jwt" && input is ToolInput.Text -> ToolBody.Jwt("", "", "", "", "")

    toolId == "regex" && input is ToolInput.Pattern -> ToolBody.Regex(
        pattern = input.pattern,
        flags = input.flags,
        segments = listOf(Segment(input.subject, matched = false)),
        matches = emptyList(),
    )

    else -> null
}

@Composable
private fun ToolHeader(tool: Tool, favorite: Boolean, onToggleFavorite: () -> Unit) {
    val colors = Nocturne.colors
    Column(Modifier.fillMaxWidth().padding(bottom = Nocturne.space.lg)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Nocturne.radii.md))
                    .background(colors.accentSurface),
            ) {
                PhosphorIcon(tool.icon, size = 19.dp, tint = colors.onAccentSurface)
            }

            Text(tool.name, style = Nocturne.type.toolTitle)
            Tag(tool.category.label)

            Box(Modifier.weight(1f))

            IconButton(
                icon = "star",
                fill = favorite,
                contentDescription =
                    if (favorite) "Remover ${tool.name} dos favoritos" else "Favoritar ${tool.name}",
                onClick = onToggleFavorite,
                boxSize = 36.dp,
                iconSize = 17.dp,
                tint = if (favorite) colors.accent else colors.text(0.45f),
            )
        }

        Text(
            tool.description,
            style = Nocturne.type.body,
            color = colors.text(0.7f),
            modifier = Modifier.padding(top = Nocturne.space.sm).widthIn(max = 620.dp),
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Nocturne.space.sm)
            .clip(RoundedCornerShape(Nocturne.radii.sm))
            .background(Nocturne.colors.mutedSurface)
            .padding(horizontal = Nocturne.space.md, vertical = Nocturne.space.xs),
    ) {
        PhosphorIcon("x-circle", size = 14.dp, fill = true, tint = Nocturne.colors.text(0.6f))
        Text(message, style = Nocturne.type.mono, color = Nocturne.colors.text(0.75f))
    }
}

@Composable
private fun ToolBodyContent(
    toolId: String,
    body: ToolBody,
    currentInput: ToolInput,
    error: String?,
    onInputChange: (ToolInput) -> Unit,
) {
    fun bumpSeed() {
        val seed = (currentInput as? ToolInput.Seed)?.nonce ?: 0
        onInputChange(ToolInput.Seed(seed + 1))
    }

    when (body) {
        is ToolBody.Io -> IoLayout(
            body = body,
            toolId = toolId,
            inputText = (currentInput as? ToolInput.Text)?.value.orEmpty(),
            onInputChange = { text ->
                val direction = (currentInput as? ToolInput.Text)?.direction ?: Direction.Auto
                onInputChange(ToolInput.Text(text, direction))
            },
        )
        is ToolBody.Jwt -> JwtLayout(
            body = body,
            toolId = toolId,
            token = (currentInput as? ToolInput.Text)?.value.orEmpty(),
            error = error,
            onTokenChange = { onInputChange(ToolInput.Text(it)) },
        )
        is ToolBody.Rows -> RowsLayout(body, toolId, onRegenerate = ::bumpSeed)
        is ToolBody.Diff -> DiffLayout(body, toolId)
        is ToolBody.Regex -> RegexLayout(
            body = body,
            toolId = toolId,
            input = currentInput as? ToolInput.Pattern
                ?: ToolInput.Pattern(body.pattern, body.flags, body.segments.joinToString("") { it.text }),
            error = error,
            onInputChange = onInputChange,
        )
        is ToolBody.Validate ->
            ValidateLayout(
                body = body,
                toolId = toolId,
                onTryAnother = { cycleExample(toolId, currentInput, onInputChange) },
            )
        is ToolBody.Qr -> QrLayout(body, toolId)
        is ToolBody.Image -> ImageLayout(
            body = body,
            toolId = toolId,
            onSelectionChange = { onInputChange(ToolInput.Image(it)) },
        )
    }
}

private val VALIDATOR_EXAMPLES: Map<String, List<String>> = mapOf(
    "cpf" to listOf("529.982.247-25", "111.444.777-30", "111.444.777-35"),
    "cnpj" to listOf("11.222.333/0001-81", "11.222.333/0001-00", "11.222.333/0002-62"),
    "phone" to listOf("(11) 98765-4321", "(21) 3456-789", "(21) 3456-7890"),
    "card" to listOf(
        "4539 5789 0080 5000",
        "5555 5555 5555 4444",
        "3782 822463 10005",
        "4539 5789 0080 5001",
    ),
)

private fun cycleExample(toolId: String, current: ToolInput, onInputChange: (ToolInput) -> Unit) {
    val examples = VALIDATOR_EXAMPLES[toolId] ?: return
    val text = (current as? ToolInput.Text)?.value ?: return
    val next = examples.getOrNull(examples.indexOf(text) + 1) ?: examples.first()
    onInputChange(ToolInput.Text(next))
}

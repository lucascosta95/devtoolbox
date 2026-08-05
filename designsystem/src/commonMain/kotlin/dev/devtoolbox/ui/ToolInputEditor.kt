package dev.devtoolbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.Card
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.CodeTextField
import dev.devtoolbox.ds.components.TextField

@Composable
fun ToolInputEditor(
    input: ToolInput,
    onChange: (ToolInput) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
) {
    when (input) {
        is ToolInput.Text -> Card(modifier.fillMaxWidth()) {
            CardKicker("Entrada")
            Box(Modifier.padding(top = Nocturne.space.xs)) {
                if (singleLine) {
                    TextField(
                        value = input.value,
                        onValueChange = { onChange(input.copy(value = it)) },
                        placeholder = "Digite aqui…",
                        framed = false,
                        textStyle = Nocturne.type.mono,
                    )
                } else {
                    CodeTextField(
                        value = input.value,
                        onValueChange = { onChange(input.copy(value = it)) },
                        placeholder = "Cole ou digite aqui…",
                        minHeight = 110.dp,
                    )
                }
            }
        }

        is ToolInput.Pair -> Row(
            modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
        ) {
            Card(Modifier.weight(1f)) {
                CardKicker("Original")
                Box(Modifier.padding(top = Nocturne.space.xs)) {
                    CodeTextField(
                        value = input.left,
                        onValueChange = { onChange(input.copy(left = it)) },
                        minHeight = 150.dp,
                    )
                }
            }
            Card(Modifier.weight(1f)) {
                CardKicker("Modificado")
                Box(Modifier.padding(top = Nocturne.space.xs)) {
                    CodeTextField(
                        value = input.right,
                        onValueChange = { onChange(input.copy(right = it)) },
                        minHeight = 150.dp,
                    )
                }
            }
        }

        is ToolInput.Pattern -> Card(modifier.fillMaxWidth()) {
            CardKicker("Entrada")
            Column(
                Modifier.padding(top = Nocturne.space.xs),
                verticalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm)) {
                    Box(Modifier.weight(1f)) {
                        TextField(
                            value = input.pattern,
                            onValueChange = { onChange(input.copy(pattern = it)) },
                            placeholder = "padrão",
                            textStyle = Nocturne.type.mono,
                        )
                    }
                    Box(Modifier.width(96.dp)) {
                        TextField(
                            value = input.flags,
                            onValueChange = { onChange(input.copy(flags = it)) },
                            placeholder = "flags",
                            textStyle = Nocturne.type.mono,
                        )
                    }
                }
                CodeTextField(
                    value = input.subject,
                    onValueChange = { onChange(input.copy(subject = it)) },
                    placeholder = "string de teste",
                    minHeight = 80.dp,
                )
            }
        }

        is ToolInput.Seed, is ToolInput.Image -> Unit
    }
}

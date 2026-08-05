package dev.devtoolbox.core

import dev.devtoolbox.core.util.EncodedImage

enum class Category(val label: String, val icon: String) {
    Encoding("Encoding", "lock-key"),
    Formatters("Formatters", "code"),
    Text("Text", "text-aa"),
    Generators("Generators", "magic-wand"),
    Validators("Validators", "check-circle"),
}

enum class Direction { Auto, Encode, Decode }

sealed interface ToolInput {
    data class Text(val value: String, val direction: Direction = Direction.Auto) : ToolInput
    data class Pair(val left: String, val right: String) : ToolInput
    data class Pattern(val pattern: String, val flags: String, val subject: String) : ToolInput
    data class Seed(val nonce: Int) : ToolInput

    data class Image(val selection: ImageSelection = ImageSelection.Empty) : ToolInput
}

sealed interface ImageSelection {
    data object Empty : ImageSelection

    data class Loading(val fileName: String) : ImageSelection

    data class Failed(val fileName: String, val message: String) : ImageSelection

    data class Loaded(val image: EncodedImage) : ImageSelection
}

sealed interface ToolOutput {
    data class Success(val body: ToolBody) : ToolOutput

    data class Failure(val message: String, val body: ToolBody? = null) : ToolOutput
}

interface Tool {
    val id: String
    val name: String
    val category: Category
    val icon: String
    val description: String
    val defaultInput: ToolInput

    val singleLineInput: Boolean get() = false

    fun run(input: ToolInput): ToolOutput
}

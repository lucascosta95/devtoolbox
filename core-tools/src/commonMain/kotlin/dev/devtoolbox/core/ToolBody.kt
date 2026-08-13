package dev.devtoolbox.core

import dev.devtoolbox.core.util.EncodedImage

data class Row(val label: String, val value: String, val swatch: String? = null)

enum class DiffKind { Same, Add, Del }

data class DiffLine(val kind: DiffKind, val text: String)

data class Segment(val text: String, val matched: Boolean)

data class MatchInfo(val index: Int, val value: String)

sealed interface ToolBody {
    data class Io(
        val input: String,
        val output: String,
        val inputLabel: String = "Entrada",
        val outputLabel: String = "Saída",
    ) : ToolBody

    data class Jwt(
        val headerPart: String,
        val payloadPart: String,
        val signaturePart: String,
        val headerJson: String,
        val payloadJson: String,
    ) : ToolBody

    data class Rows(
        val rows: List<Row>,
        val regenerable: Boolean = false,
    ) : ToolBody

    data class Diff(val lines: List<DiffLine>) : ToolBody

    data class Regex(
        val pattern: String,
        val flags: String,
        val segments: List<Segment>,
        val matches: List<MatchInfo>,
    ) : ToolBody

    data class Substring(
        val text: String,
        val start: String,
        val end: String,
        val appliedStart: Int,
        val appliedEnd: Int,
        val length: Int,
        val segments: List<Segment>,
        val result: String,
    ) : ToolBody

    data class Validate(
        val value: String,
        val valid: Boolean,
        val rows: List<Row>,
    ) : ToolBody

    data class Qr(val value: String, val modules: List<List<Boolean>>) : ToolBody

    data class Image(
        val details: ImageDetails? = null,
        val source: EncodedImage? = null,
        val loading: Boolean = false,
    ) : ToolBody
}

data class ImageDetails(
    val rows: List<Row>,
    val dataUri: String,
    val snippets: List<Row>,
)

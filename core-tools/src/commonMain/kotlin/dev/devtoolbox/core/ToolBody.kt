package dev.devtoolbox.core

/** Linha label/valor dos arquétipos `list`, `listgen` e `validate`. */
data class Row(val label: String, val value: String, val swatch: String? = null)

/** Trecho de um diff. */
enum class DiffKind { Same, Add, Del }

data class DiffLine(val kind: DiffKind, val text: String)

/** Pedaço do subject de um regex: casado ou não. */
data class Segment(val text: String, val matched: Boolean)

data class MatchInfo(val index: Int, val value: String)

/**
 * Descritor da saída de uma ferramenta — um por arquétipo de layout.
 *
 * Vive no core (sem Compose) de propósito: o composable correspondente só desenha,
 * e uma ferramenta nova vira "dados + função de transformação".
 */
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

    /** Cobre `list` e `listgen`: com [regenerable] a UI mostra "Gerar novo exemplo". */
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

    data class Validate(
        val value: String,
        val valid: Boolean,
        val rows: List<Row>,
    ) : ToolBody

    data class Qr(val value: String, val modules: List<List<Boolean>>) : ToolBody
}

package dev.devtoolbox.core.util

import dev.devtoolbox.core.DiffKind
import dev.devtoolbox.core.DiffLine

/**
 * Diff por linha via LCS (programação dinâmica).
 *
 * Saída no formato do protótipo: remoções antes das adições, linhas iguais sem marca.
 */
object Diff {

    fun lines(left: String, right: String): List<DiffLine> {
        val a = left.lines()
        val b = right.lines()
        val lcs = lcsTable(a, b)

        val out = mutableListOf<DiffLine>()
        var i = 0
        var j = 0
        while (i < a.size && j < b.size) {
            when {
                a[i] == b[j] -> { out += DiffLine(DiffKind.Same, a[i]); i++; j++ }
                lcs[i + 1][j] >= lcs[i][j + 1] -> { out += DiffLine(DiffKind.Del, a[i]); i++ }
                else -> { out += DiffLine(DiffKind.Add, b[j]); j++ }
            }
        }
        while (i < a.size) { out += DiffLine(DiffKind.Del, a[i]); i++ }
        while (j < b.size) { out += DiffLine(DiffKind.Add, b[j]); j++ }
        return reorderDeletionsFirst(out)
    }

    private fun lcsTable(a: List<String>, b: List<String>): Array<IntArray> {
        val table = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.size - 1 downTo 0) {
            for (j in b.size - 1 downTo 0) {
                table[i][j] = if (a[i] == b[j]) {
                    table[i + 1][j + 1] + 1
                } else {
                    maxOf(table[i + 1][j], table[i][j + 1])
                }
            }
        }
        return table
    }

    /** Agrupa cada bloco de mudanças com as remoções primeiro — leitura mais próxima do `diff`. */
    private fun reorderDeletionsFirst(lines: List<DiffLine>): List<DiffLine> {
        val out = mutableListOf<DiffLine>()
        var i = 0
        while (i < lines.size) {
            if (lines[i].kind == DiffKind.Same) {
                out += lines[i]; i++
            } else {
                val block = mutableListOf<DiffLine>()
                while (i < lines.size && lines[i].kind != DiffKind.Same) {
                    block += lines[i]; i++
                }
                out += block.filter { it.kind == DiffKind.Del }
                out += block.filter { it.kind == DiffKind.Add }
            }
        }
        return out
    }

    /** Normaliza os dois lados quando ambos são JSON válido, para diferenciar estrutura e não formatação. */
    fun normalizeJsonIfPossible(left: String, right: String): Pair<String, String> = try {
        Json.format(left) to Json.format(right)
    } catch (_: JsonParseException) {
        left to right
    }
}

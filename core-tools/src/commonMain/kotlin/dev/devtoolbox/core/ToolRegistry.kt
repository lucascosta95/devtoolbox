package dev.devtoolbox.core

import dev.devtoolbox.core.tools.Base64Tool
import dev.devtoolbox.core.tools.CnpjTool
import dev.devtoolbox.core.tools.ColorTool
import dev.devtoolbox.core.tools.CpfTool
import dev.devtoolbox.core.tools.CronTool
import dev.devtoolbox.core.tools.CurlFormatterTool
import dev.devtoolbox.core.tools.DiffTool
import dev.devtoolbox.core.tools.HashTool
import dev.devtoolbox.core.tools.JsonFormatterTool
import dev.devtoolbox.core.tools.JwtTool
import dev.devtoolbox.core.tools.LoremTool
import dev.devtoolbox.core.tools.PhoneTool
import dev.devtoolbox.core.tools.QrTool
import dev.devtoolbox.core.tools.RegexTool
import dev.devtoolbox.core.tools.StringCaseTool
import dev.devtoolbox.core.tools.TimestampTool
import dev.devtoolbox.core.tools.UrlTool
import dev.devtoolbox.core.tools.UuidTool
import dev.devtoolbox.core.tools.YamlFormatterTool

/**
 * Catálogo de ferramentas, na ordem exata da sidebar.
 *
 * Registrar uma ferramenta nova = uma linha em [all] + uma classe + um teste.
 */
object ToolRegistry {

    val all: List<Tool> = listOf(
        // Encoding
        Base64Tool,
        JwtTool,
        UrlTool,
        HashTool,
        // Formatters
        JsonFormatterTool,
        YamlFormatterTool,
        CurlFormatterTool,
        DiffTool,
        // Text
        StringCaseTool,
        RegexTool,
        LoremTool,
        // Generators
        UuidTool,
        ColorTool,
        TimestampTool,
        CronTool,
        QrTool,
        // Validators
        CpfTool,
        CnpjTool,
        PhoneTool,
    )

    private val byId: Map<String, Tool> = all.associateBy { it.id }

    fun byId(id: String): Tool? = byId[id]

    /** A primeira ferramenta do catálogo — usada como seleção padrão. */
    val default: Tool get() = all.first()

    /**
     * Filtra por nome **ou** categoria, `contains`, case-insensitive — mesma regra do protótipo.
     * Query em branco devolve o catálogo inteiro.
     */
    fun search(query: String): List<Tool> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter {
            it.name.lowercase().contains(q) || it.category.label.lowercase().contains(q)
        }
    }

    /** Agrupa o resultado da busca por categoria, descartando categorias vazias. */
    fun categorized(query: String = ""): List<Pair<Category, List<Tool>>> {
        val hits = search(query)
        return Category.entries
            .map { category -> category to hits.filter { it.category == category } }
            .filter { (_, tools) -> tools.isNotEmpty() }
    }
}

package dev.devtoolbox.core

/** Categorias da sidebar, na ordem exata do protótipo. */
enum class Category(val label: String, val icon: String) {
    Encoding("Encoding", "lock-key"),
    Formatters("Formatters", "code"),
    Text("Text", "text-aa"),
    Generators("Generators", "magic-wand"),
    Validators("Validators", "check-circle"),
}

/** Direção de uma ferramenta bidirecional (Base64, URL). */
enum class Direction { Auto, Encode, Decode }

/** Entrada de uma ferramenta. Uma variante por arquétipo de UI. */
sealed interface ToolInput {
    data class Text(val value: String, val direction: Direction = Direction.Auto) : ToolInput
    data class Pair(val left: String, val right: String) : ToolInput
    data class Pattern(val pattern: String, val flags: String, val subject: String) : ToolInput
    data class Seed(val nonce: Int) : ToolInput
}

/** Resultado de uma ferramenta: corpo pronto para o composable do arquétipo. */
sealed interface ToolOutput {
    data class Success(val body: ToolBody) : ToolOutput

    /** [body] opcional: permite mostrar o que deu para processar junto do erro. */
    data class Failure(val message: String, val body: ToolBody? = null) : ToolOutput
}

/**
 * Contrato de uma ferramenta.
 *
 * [run] é puro e síncrono — sem rede, sem I/O, sem estado. É o que torna cada ferramenta
 * testável em `commonTest` e o core reusável em qualquer target.
 */
interface Tool {
    val id: String
    val name: String
    val category: Category
    val icon: String
    val description: String
    val defaultInput: ToolInput

    /**
     * `true` quando a entrada é um valor único (uma cor, um CPF, uma expressão cron) — a UI
     * usa um campo de uma linha em vez do editor multilinha.
     */
    val singleLineInput: Boolean get() = false

    fun run(input: ToolInput): ToolOutput
}

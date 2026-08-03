package dev.devtoolbox.core.util

import kotlin.random.Random

/**
 * Gerador de texto de preenchimento, determinístico por semente.
 *
 * Determinismo importa: é o que permite testar o gerador sem congelar o relógio nem
 * injetar um `Random` de fora da assinatura pura de `Tool.run`.
 */
object Lorem {

    private val WORDS = listOf(
        "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed",
        "do", "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore", "magna",
        "aliqua", "enim", "ad", "minim", "veniam", "quis", "nostrud", "exercitation", "ullamco",
        "laboris", "nisi", "aliquip", "ex", "ea", "commodo", "consequat", "duis", "aute", "irure",
        "in", "reprehenderit", "voluptate", "velit", "esse", "cillum", "eu", "fugiat", "nulla",
        "pariatur", "excepteur", "sint", "occaecat", "cupidatat", "non", "proident", "sunt",
        "culpa", "qui", "officia", "deserunt", "mollit", "anim", "id", "est", "laborum",
    )

    /** Gera [paragraphs] parágrafos com 3 a 5 frases cada. */
    fun paragraphs(count: Int, seed: Int): List<String> {
        val random = Random(seed)
        return (0 until count).map { index ->
            val sentences = (0 until random.nextInt(3, 6)).map { sentence(random) }
            // O primeiro parágrafo começa com a abertura clássica.
            if (index == 0) {
                ("Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    sentences.joinToString(" ")).trim()
            } else {
                sentences.joinToString(" ")
            }
        }
    }

    private fun sentence(random: Random): String {
        val length = random.nextInt(6, 14)
        val words = (0 until length).map { WORDS[random.nextInt(WORDS.size)] }
        val text = words.joinToString(" ")
        return text.replaceFirstChar { it.uppercase() } + "."
    }
}

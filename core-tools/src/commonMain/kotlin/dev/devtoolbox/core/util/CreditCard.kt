package dev.devtoolbox.core.util

/**
 * Validação de número de cartão pelo algoritmo de Luhn, com detecção de bandeira por BIN.
 *
 * O veredito é **só** o Luhn (mais comprimento e caracteres aceitos): a bandeira é informativa.
 * Um número com dígito verificador correto continua válido mesmo que o comprimento fuja do
 * usual para a bandeira detectada — isso vira uma observação na linha "Bandeira", não uma
 * reprovação, porque emissores lançam faixas novas o tempo todo.
 */
object CreditCard {

    /** Faixa aceita pela ISO/IEC 7812 — nenhum cartão real fica fora dela. */
    private const val MIN_DIGITS = 12
    private const val MAX_DIGITS = 19

    /**
     * Bandeira: [prefixes] casa por prefixo literal, [ranges] por faixa numérica do BIN
     * (comparada com os primeiros dígitos, no comprimento do próprio limite).
     */
    data class Brand(
        val name: String,
        val prefixes: List<String> = emptyList(),
        val ranges: List<IntRange> = emptyList(),
        val lengths: List<Int>,
    ) {
        fun matches(digits: String): Boolean =
            prefixes.any { digits.startsWith(it) } || ranges.any { range ->
                val width = range.last.toString().length
                val head = digits.take(width).toIntOrNull()
                head != null && digits.length >= width && head in range
            }
    }

    /**
     * Ordem importa: Elo e Hipercard emitem dentro de faixas que começam com 4, 5 e 6 e
     * seriam engolidas por Visa/Mastercard/Discover se viessem depois.
     */
    val brands: List<Brand> = listOf(
        Brand(
            name = "Elo",
            prefixes = listOf(
                "401178", "401179", "431274", "438935", "451416", "457393", "457631", "457632",
                "504175", "627780", "636297", "636368",
            ),
            ranges = listOf(
                506699..506778, 509000..509999, 650031..650033, 650035..650051,
                650405..650439, 650485..650538, 650541..650598, 650700..650718,
                650720..650727, 650901..650978, 651652..651679, 655000..655019,
                655021..655058,
            ),
            lengths = listOf(16),
        ),
        Brand("Hipercard", prefixes = listOf("606282", "3841"), lengths = listOf(16, 19)),
        Brand("Visa", prefixes = listOf("4"), lengths = listOf(13, 16, 19)),
        Brand(
            name = "Mastercard",
            ranges = listOf(51..55, 2221..2720),
            lengths = listOf(16),
        ),
        Brand("American Express", prefixes = listOf("34", "37"), lengths = listOf(15)),
        Brand("Diners Club", prefixes = listOf("36", "38"), ranges = listOf(300..305), lengths = listOf(14, 16)),
        Brand(
            name = "Discover",
            prefixes = listOf("6011", "65"),
            ranges = listOf(644..649, 622126..622925),
            lengths = listOf(16, 19),
        ),
        Brand("JCB", ranges = listOf(3528..3589), lengths = listOf(16, 19)),
    )

    fun brandOf(digits: String): Brand? = brands.firstOrNull { it.matches(digits) }

    /**
     * Soma de Luhn: da direita para a esquerda, cada segundo dígito dobra e, passando de 9,
     * perde 9 (equivale a somar os algarismos do dobro). Válido quando a soma é múltipla de 10.
     */
    fun luhnSum(digits: String): Int =
        digits.reversed().foldIndexed(0) { index, acc, char ->
            val digit = char - '0'
            val weighted = if (index % 2 == 1) (digit * 2).let { if (it > 9) it - 9 else it } else digit
            acc + weighted
        }

    fun isValid(input: String): Boolean {
        val digits = normalize(input) ?: return false
        return digits.length in MIN_DIGITS..MAX_DIGITS && luhnSum(digits) % 10 == 0
    }

    /** Tira espaços e hifens; devolve `null` se sobrar qualquer coisa que não seja dígito. */
    fun normalize(input: String): String? {
        val stripped = input.trim().filterNot { it == ' ' || it == '-' }
        return if (stripped.isNotEmpty() && stripped.all { it.isDigit() }) stripped else null
    }

    /** Dígito verificador que tornaria [payload] (o número sem o último dígito) válido. */
    fun expectedCheckDigit(payload: String): Int {
        val sum = payload.reversed().foldIndexed(0) { index, acc, char ->
            val digit = char - '0'
            // O payload está deslocado uma casa: o que aqui é índice par dobra.
            val weighted = if (index % 2 == 0) (digit * 2).let { if (it > 9) it - 9 else it } else digit
            acc + weighted
        }
        return (10 - sum % 10) % 10
    }

    fun validate(input: String): ValidationResult {
        val digits = normalize(input)
            ?: return ValidationResult(
                valid = false,
                formatted = input.trim(),
                details = listOf(
                    "Somente números" to input.filter { it.isDigit() },
                    "Motivo" to "o número aceita apenas dígitos, espaços e hifens",
                ),
            )

        if (digits.length !in MIN_DIGITS..MAX_DIGITS) {
            return ValidationResult(
                valid = false,
                formatted = format(digits),
                details = listOf(
                    "Somente números" to digits,
                    "Bandeira" to brandLine(digits),
                    "Motivo" to
                        "comprimento fora da faixa: esperado de $MIN_DIGITS a $MAX_DIGITS dígitos, " +
                        "recebido ${digits.length}",
                ),
            )
        }

        val sum = luhnSum(digits)
        val multiple = sum % 10 == 0
        val sumLine = "$sum — ${if (multiple) "múltiplo de 10" else "não é múltiplo de 10"}"
        val expected = expectedCheckDigit(digits.dropLast(1))
        val received = digits.last() - '0'

        val lastRow = if (multiple) {
            "Dígito verificador" to "$received — válido"
        } else {
            "Motivo" to "Dígito verificador incorreto: esperado $expected, recebido $received"
        }

        return ValidationResult(
            valid = multiple,
            formatted = format(digits),
            details = listOf(
                "Somente números" to digits,
                "Bandeira" to brandLine(digits),
                "Soma de Luhn" to sumLine,
                lastRow,
            ),
        )
    }

    private fun brandLine(digits: String): String {
        val brand = brandOf(digits)
            ?: return "não identificada (BIN ${digits.take(6)})"
        val bin = digits.take(4)
        val expected = brand.lengths.joinToString(" ou ")
        return if (digits.length in brand.lengths) {
            "${brand.name} (BIN $bin, ${digits.length} dígitos)"
        } else {
            "${brand.name} (BIN $bin, ${digits.length} dígitos — usual: $expected)"
        }
    }

    /** Agrupa em blocos de 4 — Amex, com 15 dígitos, usa o 4-6-5 tradicional. */
    fun format(digits: String): String = when {
        digits.length == 15 -> listOf(
            digits.take(4),
            digits.substring(4, 10),
            digits.substring(10),
        ).joinToString(" ")
        else -> digits.chunked(4).joinToString(" ")
    }
}

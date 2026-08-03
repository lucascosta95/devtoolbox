package dev.devtoolbox.core.util

/**
 * Validadores de CPF, CNPJ e telefone brasileiro.
 *
 * Cada um devolve o veredito **e** o detalhamento que a UI mostra em linhas label/valor,
 * incluindo o motivo quando o valor é inválido.
 */
data class ValidationResult(
    val valid: Boolean,
    val formatted: String,
    val details: List<Pair<String, String>>,
)

object Cpf {

    fun validate(input: String): ValidationResult {
        val digits = input.filter { it.isDigit() }

        if (digits.length != 11) {
            return ValidationResult(
                valid = false,
                formatted = input.trim(),
                details = listOf(
                    "Somente números" to digits,
                    "Motivo" to "CPF precisa ter 11 dígitos — recebidos ${digits.length}",
                ),
            )
        }

        if (digits.all { it == digits[0] }) {
            return ValidationResult(
                valid = false,
                formatted = format(digits),
                details = listOf(
                    "Somente números" to digits,
                    "Motivo" to "todos os dígitos iguais — sequência inválida",
                ),
            )
        }

        val d1 = checkDigit(digits.take(9), 10)
        val d2 = checkDigit(digits.take(9) + d1, 11)
        val expected = "$d1$d2"
        val received = digits.takeLast(2)

        return if (expected == received) {
            ValidationResult(
                valid = true,
                formatted = format(digits),
                details = listOf(
                    "Somente números" to digits,
                    "Dígitos verificadores" to "$d1, $d2 — válidos",
                    "Formatado" to format(digits),
                ),
            )
        } else {
            ValidationResult(
                valid = false,
                formatted = format(digits),
                details = listOf(
                    "Somente números" to digits,
                    "Dígitos verificadores" to "$expected esperado, recebido $received",
                    "Motivo" to
                        if (expected[0] != received[0]) "Primeiro dígito verificador incorreto"
                        else "Segundo dígito verificador incorreto",
                ),
            )
        }
    }

    /** Soma ponderada com pesos decrescentes a partir de [startWeight]; resto < 2 vira 0. */
    private fun checkDigit(digits: String, startWeight: Int): Int {
        val sum = digits.mapIndexed { i, c -> (c - '0') * (startWeight - i) }.sum()
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }

    fun format(digits: String): String =
        "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9)}"
}

object Cnpj {

    private val WEIGHTS_1 = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    private val WEIGHTS_2 = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)

    fun validate(input: String): ValidationResult {
        val digits = input.filter { it.isDigit() }

        if (digits.length != 14) {
            return ValidationResult(
                valid = false,
                formatted = input.trim(),
                details = listOf(
                    "Somente números" to digits,
                    "Motivo" to "CNPJ precisa ter 14 dígitos — recebidos ${digits.length}",
                ),
            )
        }

        if (digits.all { it == digits[0] }) {
            return ValidationResult(
                valid = false,
                formatted = format(digits),
                details = listOf(
                    "Somente números" to digits,
                    "Motivo" to "todos os dígitos iguais — sequência inválida",
                ),
            )
        }

        val branch = digits.substring(8, 12)
        val branchLabel = if (branch == "0001") "$branch (matriz)" else "$branch (filial)"

        val d1 = checkDigit(digits.take(12), WEIGHTS_1)
        val d2 = checkDigit(digits.take(12) + d1, WEIGHTS_2)
        val expected = "$d1$d2"
        val received = digits.takeLast(2)

        return if (expected == received) {
            ValidationResult(
                valid = true,
                formatted = format(digits),
                details = listOf(
                    "Somente números" to digits,
                    "Filial" to branchLabel,
                    "Dígitos verificadores" to "$d1, $d2 — válidos",
                ),
            )
        } else {
            ValidationResult(
                valid = false,
                formatted = format(digits),
                details = listOf(
                    "Somente números" to digits,
                    "Filial" to branchLabel,
                    "Dígitos verificadores" to "$expected esperado, recebido $received",
                    "Motivo" to "Dígitos verificadores incorretos",
                ),
            )
        }
    }

    private fun checkDigit(digits: String, weights: IntArray): Int {
        val sum = digits.mapIndexed { i, c -> (c - '0') * weights[i] }.sum()
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }

    fun format(digits: String): String =
        "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/" +
            "${digits.substring(8, 12)}-${digits.substring(12)}"
}

object BrazilianPhone {

    /** DDDs válidos e a praça correspondente (Anatel). */
    private val AREA_CODES: Map<Int, String> = mapOf(
        11 to "São Paulo", 12 to "São José dos Campos", 13 to "Santos", 14 to "Bauru",
        15 to "Sorocaba", 16 to "Ribeirão Preto", 17 to "São José do Rio Preto", 18 to "Presidente Prudente",
        19 to "Campinas", 21 to "Rio de Janeiro", 22 to "Campos dos Goytacazes", 24 to "Volta Redonda",
        27 to "Vitória", 28 to "Cachoeiro de Itapemirim", 31 to "Belo Horizonte", 32 to "Juiz de Fora",
        33 to "Governador Valadares", 34 to "Uberlândia", 35 to "Poços de Caldas", 37 to "Divinópolis",
        38 to "Montes Claros", 41 to "Curitiba", 42 to "Ponta Grossa", 43 to "Londrina",
        44 to "Maringá", 45 to "Foz do Iguaçu", 46 to "Pato Branco", 47 to "Joinville",
        48 to "Florianópolis", 49 to "Chapecó", 51 to "Porto Alegre", 53 to "Pelotas",
        54 to "Caxias do Sul", 55 to "Santa Maria", 61 to "Brasília", 62 to "Goiânia",
        63 to "Palmas", 64 to "Rio Verde", 65 to "Cuiabá", 66 to "Rondonópolis",
        67 to "Campo Grande", 68 to "Rio Branco", 69 to "Porto Velho", 71 to "Salvador",
        73 to "Itabuna", 74 to "Juazeiro", 75 to "Feira de Santana", 77 to "Barreiras",
        79 to "Aracaju", 81 to "Recife", 82 to "Maceió", 83 to "João Pessoa",
        84 to "Natal", 85 to "Fortaleza", 86 to "Teresina", 87 to "Petrolina",
        88 to "Juazeiro do Norte", 89 to "Picos", 91 to "Belém", 92 to "Manaus",
        93 to "Santarém", 94 to "Marabá", 95 to "Boa Vista", 96 to "Macapá",
        97 to "Tefé", 98 to "São Luís", 99 to "Imperatriz",
    )

    fun validate(input: String): ValidationResult {
        var digits = input.filter { it.isDigit() }
        // Aceita +55 na frente, desde que sobre um número plausível.
        if (digits.startsWith("55") && digits.length > 11) digits = digits.drop(2)

        if (digits.length < 10) {
            return ValidationResult(
                valid = false,
                formatted = input.trim(),
                details = listOf(
                    "Somente números" to digits,
                    "Motivo" to
                        "esperado DDD + 8 ou 9 dígitos — recebidos ${digits.length} dígitos no total",
                ),
            )
        }

        val ddd = digits.take(2).toInt()
        val subscriber = digits.drop(2)
        val city = AREA_CODES[ddd]

        if (city == null) {
            return ValidationResult(
                valid = false,
                formatted = input.trim(),
                details = listOf(
                    "DDD" to "$ddd — não existe",
                    "Motivo" to "DDD inválido",
                ),
            )
        }

        val mobile = subscriber.length == 9
        val landline = subscriber.length == 8

        if (!mobile && !landline) {
            return ValidationResult(
                valid = false,
                formatted = input.trim(),
                details = listOf(
                    "DDD" to "$ddd — $city",
                    "Tipo" to "quantidade de dígitos inválida",
                    "Motivo" to "esperado 8 (fixo) ou 9 (celular) dígitos após o DDD, " +
                        "recebido ${subscriber.length}",
                ),
            )
        }

        if (mobile && subscriber.first() != '9') {
            return ValidationResult(
                valid = false,
                formatted = format(ddd, subscriber),
                details = listOf(
                    "DDD" to "$ddd — $city",
                    "Tipo" to "Celular (9 dígitos)",
                    "Motivo" to "celular precisa começar com 9",
                ),
            )
        }

        if (landline && subscriber.first() !in '2'..'5') {
            return ValidationResult(
                valid = false,
                formatted = format(ddd, subscriber),
                details = listOf(
                    "DDD" to "$ddd — $city",
                    "Tipo" to "Fixo (8 dígitos)",
                    "Motivo" to "telefone fixo começa com dígito de 2 a 5",
                ),
            )
        }

        return ValidationResult(
            valid = true,
            formatted = format(ddd, subscriber),
            details = listOf(
                "DDD" to "$ddd — $city",
                "Tipo" to if (mobile) "Celular (9 dígitos)" else "Fixo (8 dígitos)",
                "E.164" to "+55$ddd$subscriber",
            ),
        )
    }

    fun format(ddd: Int, subscriber: String): String {
        val head = if (subscriber.length == 9) subscriber.take(5) else subscriber.take(4)
        val tail = subscriber.drop(head.length)
        return "($ddd) $head-$tail"
    }
}

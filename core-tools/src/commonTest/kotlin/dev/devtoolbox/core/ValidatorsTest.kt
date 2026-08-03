package dev.devtoolbox.core

import dev.devtoolbox.core.util.BrazilianPhone
import dev.devtoolbox.core.util.Cnpj
import dev.devtoolbox.core.util.Cpf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CpfTest {

    @Test
    fun acceptsValidCpfFormattedOrNot() {
        assertTrue(Cpf.validate("529.982.247-25").valid)
        assertTrue(Cpf.validate("52998224725").valid)
        assertTrue(Cpf.validate(" 529 982 247 25 ").valid)
    }

    @Test
    fun rejectsWrongCheckDigit() {
        val result = Cpf.validate("111.444.777-30")
        assertFalse(result.valid)
        assertTrue(result.details.any { it.second.contains("dígito verificador", ignoreCase = true) })
    }

    @Test
    fun acceptsTheKnownValidVariantOfTheSameBase() {
        // 111.444.777-35 é o CPF válido para essa base; -30 não é.
        assertTrue(Cpf.validate("111.444.777-35").valid)
    }

    @Test
    fun rejectsRepeatedDigits() {
        for (d in '0'..'9') {
            val result = Cpf.validate(d.toString().repeat(11))
            assertFalse(result.valid, "$d repetido deveria ser inválido")
            assertTrue(result.details.any { it.second.contains("iguais") })
        }
    }

    @Test
    fun rejectsWrongLength() {
        val result = Cpf.validate("529982247")
        assertFalse(result.valid)
        assertTrue(result.details.any { it.second.contains("11 dígitos") })
    }

    @Test
    fun anyBaseWithItsComputedDigitsIsValid() {
        // Propriedade do algoritmo: os DVs calculados sempre fecham a validação — inclusive
        // nas bases cujo resto < 2 produz dígito 0.
        var checked = 0
        for (base in listOf("529982247", "111444777", "000000001", "123456789", "987654321")) {
            if (base.all { it == base[0] }) continue
            val result = Cpf.validate(base + digitsFor(base))
            assertTrue(result.valid, "base $base com DVs calculados deveria ser válida")
            checked++
        }
        assertEquals(5, checked)
    }

    /** Reimplementação independente dos DVs, para não testar o código com ele mesmo. */
    private fun digitsFor(base: String): String {
        fun dv(digits: String, start: Int): Int {
            var sum = 0
            for ((i, c) in digits.withIndex()) sum += (c - '0') * (start - i)
            val rest = sum % 11
            return if (rest < 2) 0 else 11 - rest
        }
        val d1 = dv(base, 10)
        return "$d1${dv(base + d1, 11)}"
    }

    @Test
    fun formatsDigitsWithPunctuation() {
        assertEquals("000.000.001-91", Cpf.format("00000000191"))
    }
}

class CnpjTest {

    @Test
    fun acceptsValidCnpj() {
        assertTrue(Cnpj.validate("11.222.333/0001-81").valid)
        assertTrue(Cnpj.validate("11222333000181").valid)
    }

    @Test
    fun rejectsWrongCheckDigits() {
        val result = Cnpj.validate("11.222.333/0001-00")
        assertFalse(result.valid)
        assertTrue(result.details.any { it.first == "Dígitos verificadores" })
    }

    @Test
    fun identifiesHeadquartersAndBranch() {
        val hq = Cnpj.validate("11.222.333/0001-81")
        assertTrue(hq.details.any { it.second.contains("matriz") })

        val branch = Cnpj.validate("11.222.333/0002-62")
        assertTrue(branch.details.any { it.second.contains("filial") })
    }

    @Test
    fun rejectsRepeatedDigits() {
        assertFalse(Cnpj.validate("11.111.111/1111-11").valid)
    }

    @Test
    fun rejectsWrongLength() {
        val result = Cnpj.validate("112223330001")
        assertFalse(result.valid)
        assertTrue(result.details.any { it.second.contains("14 dígitos") })
    }
}

class BrazilianPhoneTest {

    @Test
    fun acceptsMobileWithNineDigits() {
        val result = BrazilianPhone.validate("(11) 98765-4321")
        assertTrue(result.valid)
        assertTrue(result.details.any { it.second == "+5511987654321" })
        assertTrue(result.details.any { it.second.contains("São Paulo") })
    }

    @Test
    fun acceptsLandlineWithEightDigits() {
        val result = BrazilianPhone.validate("(21) 3456-7890")
        assertTrue(result.valid)
        assertTrue(result.details.any { it.second.contains("Fixo") })
    }

    @Test
    fun acceptsE164WithCountryCode() {
        assertTrue(BrazilianPhone.validate("+55 11 98765-4321").valid)
    }

    @Test
    fun rejectsWrongDigitCount() {
        val result = BrazilianPhone.validate("(21) 3456-789")
        assertFalse(result.valid)
        assertTrue(result.details.any { it.second.contains("recebido") })
    }

    @Test
    fun rejectsUnknownAreaCode() {
        val result = BrazilianPhone.validate("(23) 98765-4321")
        assertFalse(result.valid)
        assertTrue(result.details.any { it.second.contains("não existe") })
    }

    @Test
    fun rejectsMobileNotStartingWithNine() {
        val result = BrazilianPhone.validate("(11) 88765-4321")
        assertFalse(result.valid)
        assertTrue(result.details.any { it.second.contains("começar com 9") })
    }

    @Test
    fun formatsBothLengths() {
        assertEquals("(11) 98765-4321", BrazilianPhone.format(11, "987654321"))
        assertEquals("(21) 3456-7890", BrazilianPhone.format(21, "34567890"))
    }
}

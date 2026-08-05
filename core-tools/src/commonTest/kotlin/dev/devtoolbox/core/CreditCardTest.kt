package dev.devtoolbox.core

import dev.devtoolbox.core.tools.CardTool
import dev.devtoolbox.core.util.CreditCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreditCardTest {

    @Test
    fun acceptsWellKnownTestNumbers() {
        assertTrue(CreditCard.isValid("4539578900805000"), "Visa")
        assertTrue(CreditCard.isValid("5555555555554444"), "Mastercard")
        assertTrue(CreditCard.isValid("378282246310005"), "Amex")
        assertTrue(CreditCard.isValid("6011111111111117"), "Discover")
        assertTrue(CreditCard.isValid("30569309025904"), "Diners")
    }

    @Test
    fun rejectsNumbersWithAWrongCheckDigit() {
        assertFalse(CreditCard.isValid("4539578900805001"))
        assertFalse(CreditCard.isValid("5555555555554445"))
        assertFalse(CreditCard.isValid("378282246310006"))
    }

    @Test
    fun rejectsLengthsOutsideTheAcceptedRange() {
        assertFalse(CreditCard.isValid("4539578900"), "11 dígitos é curto demais")
        assertFalse(CreditCard.isValid("45395789008050001234"), "20 dígitos é longo demais")
        assertFalse(CreditCard.isValid(""))
    }

    @Test
    fun rejectsInvalidCharactersButAcceptsSpacesAndHyphens() {
        assertNull(CreditCard.normalize("4539 5789 0080 500A"))
        assertNull(CreditCard.normalize("4539.5789.0080.5000"))
        assertEquals("4539578900805000", CreditCard.normalize("4539 5789 0080 5000"))
        assertEquals("4539578900805000", CreditCard.normalize("4539-5789-0080-5000"))
        assertTrue(CreditCard.isValid(" 4539-5789 0080-5000 "))
    }

    @Test
    fun luhnSumIsAMultipleOfTenOnlyForValidNumbers() {
        assertEquals(0, CreditCard.luhnSum("4539578900805000") % 10)
        assertEquals(1, CreditCard.luhnSum("4539578900805001") % 10)
    }

    @Test
    fun computesTheCheckDigitThatWouldMakeTheNumberValid() {
        assertEquals(0, CreditCard.expectedCheckDigit("453957890080500"))
        assertEquals(4, CreditCard.expectedCheckDigit("555555555555444"))
        assertEquals(5, CreditCard.expectedCheckDigit("37828224631000"))
    }

    @Test
    fun detectsBrandsByBin() {
        fun brand(digits: String) = CreditCard.brandOf(digits)?.name

        assertEquals("Visa", brand("4539578900805000"))
        assertEquals("Mastercard", brand("5555555555554444"))
        assertEquals("Mastercard", brand("2221000000000009"))
        assertEquals("American Express", brand("378282246310005"))
        assertEquals("American Express", brand("341111111111111"))
        assertEquals("Diners Club", brand("30569309025904"))
        assertEquals("Diners Club", brand("36700102000000"))
        assertEquals("Discover", brand("6011111111111117"))
        assertEquals("JCB", brand("3530111333300000"))
        assertEquals("Elo", brand("4011780000000000"))
        assertEquals("Elo", brand("5066990000000000"))
        assertEquals("Hipercard", brand("6062820000000000"))
        assertNull(CreditCard.brandOf("9999999999999999"))
    }

    @Test
    fun elpWinsOverVisaAndMastercardOnItsOwnBins() {
        assertEquals("Elo", CreditCard.brandOf("4011781234567890")?.name)
        assertEquals("Elo", CreditCard.brandOf("5066991234567890")?.name)
        assertEquals("Visa", CreditCard.brandOf("4011771234567890")?.name)
    }

    @Test
    fun detailsDescribeTheBrandLuhnSumAndCheckDigit() {
        val result = CreditCard.validate("4539 5789 0080 5000")
        val details = result.details.toMap()

        assertTrue(result.valid)
        assertEquals("4539578900805000", details["Somente números"])
        assertEquals("Visa (BIN 4539, 16 dígitos)", details["Bandeira"])
        assertEquals("60 — múltiplo de 10", details["Soma de Luhn"])
        assertEquals("0 — válido", details["Dígito verificador"])
    }

    @Test
    fun detailsExplainWhyAnInvalidCheckDigitFailed() {
        val details = CreditCard.validate("4539 5789 0080 5001").details.toMap()

        assertEquals("61 — não é múltiplo de 10", details["Soma de Luhn"])
        assertEquals(
            "Dígito verificador incorreto: esperado 0, recebido 1",
            details["Motivo"],
        )
    }

    @Test
    fun detailsExplainLengthAndCharacterFailures() {
        val short = CreditCard.validate("4539 5789").details.toMap()
        assertTrue(short.getValue("Motivo").contains("comprimento fora da faixa"))
        assertTrue(short.getValue("Motivo").contains("recebido 8"))

        val letters = CreditCard.validate("4539 5789 0080 500A").details.toMap()
        assertTrue(letters.getValue("Motivo").contains("apenas dígitos"))
    }

    @Test
    fun formatsAmexAsFourSixFiveAndTheRestInBlocksOfFour() {
        assertEquals("3782 822463 10005", CreditCard.format("378282246310005"))
        assertEquals("4539 5789 0080 5000", CreditCard.format("4539578900805000"))
    }

    @Test
    fun toolExposesTheValidationAsAValidateBody() {
        val output = CardTool.run(ToolInput.Text("4539 5789 0080 5000"))
        val body = (output as ToolOutput.Success).body as ToolBody.Validate

        assertTrue(body.valid)
        assertEquals("4539 5789 0080 5000", body.value)
        assertEquals(
            listOf("Somente números", "Bandeira", "Soma de Luhn", "Dígito verificador"),
            body.rows.map { it.label },
        )
    }

    @Test
    fun toolReportsInvalidNumbers() {
        val output = CardTool.run(ToolInput.Text("5555 5555 5555 4445"))
        val body = (output as ToolOutput.Success).body as ToolBody.Validate

        assertFalse(body.valid)
        assertEquals("Motivo", body.rows.last().label)
    }

    @Test
    fun toolIsRegisteredUnderValidators() {
        val tool = ToolRegistry.byId("card")
        assertEquals(Category.Validators, tool?.category)
        assertEquals("credit-card", tool?.icon)
    }
}

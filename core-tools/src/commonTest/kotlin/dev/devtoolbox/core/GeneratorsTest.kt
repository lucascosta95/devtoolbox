package dev.devtoolbox.core

import dev.devtoolbox.core.tools.ColorTool
import dev.devtoolbox.core.tools.CronTool
import dev.devtoolbox.core.tools.LoremTool
import dev.devtoolbox.core.tools.QrTool
import dev.devtoolbox.core.tools.RegexTool
import dev.devtoolbox.core.tools.StringCaseTool
import dev.devtoolbox.core.tools.TimestampTool
import dev.devtoolbox.core.tools.UuidTool
import dev.devtoolbox.core.util.ColorConvert
import dev.devtoolbox.core.util.ColorParseException
import dev.devtoolbox.core.util.Cron
import dev.devtoolbox.core.util.CronParseException
import dev.devtoolbox.core.util.QrCode
import dev.devtoolbox.core.util.ReedSolomon
import dev.devtoolbox.core.util.StringCase
import dev.devtoolbox.core.util.Uuid
import kotlinx.datetime.TimeZone
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class StringCaseTest {

    @Test
    fun convertsThePrototypeExample() {
        val out = StringCaseTool.run(ToolInput.Text("hello devtoolbox world"))
        assertIs<ToolOutput.Success>(out)
        val rows = (out.body as ToolBody.Rows).rows.associate { it.label to it.value }
        assertEquals("hello devtoolbox world", rows["lower case"])
        assertEquals("HELLO DEVTOOLBOX WORLD", rows["UPPER CASE"])
        assertEquals("Hello Devtoolbox World", rows["Capitalized"])
        assertEquals("helloDevtoolboxWorld", rows["camelCase"])
        assertEquals("HelloDevtoolboxWorld", rows["PascalCase"])
        assertEquals("hello_devtoolbox_world", rows["snake_case"])
        assertEquals("hello-devtoolbox-world", rows["kebab-case"])
        assertEquals("HELLO_DEVTOOLBOX_WORLD", rows["CONSTANT_CASE"])
    }

    @Test
    fun splitsOnCamelBoundaries() {
        assertEquals("hello_world_again", StringCase.snake("helloWorldAgain"))
        assertEquals("hello-world", StringCase.kebab("HelloWorld"))
    }

    @Test
    fun splitsAcronymsAtTheirEnd() {
        assertEquals(listOf("HTTP", "Server"), StringCase.tokenize("HTTPServer"))
    }

    @Test
    fun acceptsMixedSeparators() {
        assertEquals("a_b_c_d", StringCase.snake("a-b_c.d"))
    }

    @Test
    fun keepsAccents() {
        assertEquals("ação_rápida", StringCase.snake("Ação Rápida"))
    }

    @Test
    fun handlesEmptyInput() {
        assertEquals("", StringCase.camel(""))
        assertEquals("", StringCase.snake("   "))
    }
}

class RegexToolTest {

    @Test
    fun findsAllMatchesWithGlobalFlag() {
        val out = RegexTool.run(RegexTool.defaultInput)
        assertIs<ToolOutput.Success>(out)
        val body = out.body as ToolBody.Regex
        assertEquals(listOf("555-0132", "555-0148"), body.matches.map { it.value })
        assertEquals(listOf(0, 1), body.matches.map { it.index })
    }

    @Test
    fun withoutGlobalFlagOnlyTheFirstMatchCounts() {
        val out = RegexTool.run(ToolInput.Pattern("""\d+""", "", "1 2 3"))
        assertIs<ToolOutput.Success>(out)
        assertEquals(1, (out.body as ToolBody.Regex).matches.size)
    }

    @Test
    fun segmentsCoverTheWholeSubject() {
        val out = RegexTool.run(RegexTool.defaultInput)
        assertIs<ToolOutput.Success>(out)
        val body = out.body as ToolBody.Regex
        val rebuilt = body.segments.joinToString("") { it.text }
        assertEquals("Suporte: 555-0132 ou 555-0148.", rebuilt)
        assertEquals(2, body.segments.count { it.matched })
    }

    @Test
    fun ignoreCaseFlagWorks() {
        val out = RegexTool.run(ToolInput.Pattern("abc", "gi", "ABC abc"))
        assertIs<ToolOutput.Success>(out)
        assertEquals(2, (out.body as ToolBody.Regex).matches.size)
    }

    @Test
    fun reportsInvalidPattern() {
        val out = RegexTool.run(ToolInput.Pattern("(unclosed", "g", "x"))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("Regex inválida"))
    }

    @Test
    fun zeroMatchesIsSuccessWithEmptyList() {
        val out = RegexTool.run(ToolInput.Pattern("zzz", "g", "abc"))
        assertIs<ToolOutput.Success>(out)
        val body = out.body as ToolBody.Regex
        assertTrue(body.matches.isEmpty())
        assertEquals("abc", body.segments.single().text)
    }
}

class LoremToolTest {

    @Test
    fun isDeterministicForTheSameSeed() {
        assertEquals(LoremTool.run(ToolInput.Seed(7)), LoremTool.run(ToolInput.Seed(7)))
    }

    @Test
    fun differentSeedsProduceDifferentText() {
        val a = LoremTool.run(ToolInput.Seed(1)) as ToolOutput.Success
        val b = LoremTool.run(ToolInput.Seed(2)) as ToolOutput.Success
        assertNotEquals(a.body, b.body)
    }

    @Test
    fun firstParagraphOpensWithTheClassicLine() {
        val out = LoremTool.run(ToolInput.Seed(0)) as ToolOutput.Success
        val rows = (out.body as ToolBody.Rows).rows
        assertTrue(rows.first().value.startsWith("Lorem ipsum dolor sit amet"))
        assertEquals(3, rows.size)
    }
}

class UuidToolTest {

    private val v4Pattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

    @Test
    fun generatesCanonicalV4() {
        val out = UuidTool.run(ToolInput.Seed(0)) as ToolOutput.Success
        val rows = (out.body as ToolBody.Rows).rows
        assertEquals(3, rows.size)
        for (row in rows) {
            assertTrue(v4Pattern.matches(row.value), "UUID fora do formato v4: ${row.value}")
        }
    }

    @Test
    fun versionAndVariantBitsAreSetForManySeeds() {
        for (seed in 0 until 200) {
            val uuid = Uuid.v4(Random(seed))
            assertTrue(v4Pattern.matches(uuid), "semente $seed gerou $uuid")
        }
    }

    @Test
    fun isDeterministicForTheSameSeed() {
        assertEquals(Uuid.v4Batch(3, 42), Uuid.v4Batch(3, 42))
        assertNotEquals(Uuid.v4Batch(3, 42), Uuid.v4Batch(3, 43))
    }
}

class ColorToolTest {

    @Test
    fun convertsThePrototypeColor() {
        val out = ColorTool.run(ToolInput.Text("#9184d9")) as ToolOutput.Success
        val rows = (out.body as ToolBody.Rows).rows.associate { it.label to it.value }
        assertEquals("#9184d9", rows["HEX"])
        assertEquals("rgb(145, 132, 217)", rows["RGB"])
        assertEquals("hsl(249, 53%, 68%)", rows["HSL"])
        val oklch = rows["OKLCH"]!!
        val numbers = Regex("[\\d.]+").findAll(oklch).map { it.value.toDouble() }.toList()
        assertTrue(abs(numbers[0] - 66.0) < 0.1, "L fora do esperado: $oklch")
        assertTrue(abs(numbers[1] - 0.1245) < 0.001, "C fora do esperado: $oklch")
        assertTrue(abs(numbers[2] - 289.55) < 0.5, "H fora do esperado: $oklch")
    }

    @Test
    fun roundTripsThroughEveryFormat() {
        for (hex in listOf("#9184d9", "#e4b73c", "#000000", "#ffffff", "#ff0000", "#1a2b3c")) {
            val rgb = ColorConvert.parse(hex)
            assertEquals(rgb, ColorConvert.parse(ColorConvert.toRgbString(rgb)), "rgb $hex")
            val backFromHsl = ColorConvert.parse(ColorConvert.toHslString(rgb))
            assertTrue(abs(backFromHsl.r - rgb.r) <= 3, "hsl r de $hex")
            assertTrue(abs(backFromHsl.g - rgb.g) <= 3, "hsl g de $hex")
            assertTrue(abs(backFromHsl.b - rgb.b) <= 3, "hsl b de $hex")

            val backFromOklch = ColorConvert.parse(ColorConvert.toOklchString(rgb))
            assertTrue(abs(backFromOklch.r - rgb.r) <= 2, "oklch r de $hex")
            assertTrue(abs(backFromOklch.g - rgb.g) <= 2, "oklch g de $hex")
            assertTrue(abs(backFromOklch.b - rgb.b) <= 2, "oklch b de $hex")
        }
    }

    @Test
    fun acceptsShortHexAndBareHex() {
        assertEquals(ColorConvert.parse("#ffcc00"), ColorConvert.parse("#fc0"))
        assertEquals(ColorConvert.parse("#ffcc00"), ColorConvert.parse("ffcc00"))
    }

    @Test
    fun rejectsMalformedColors() {
        assertFailsWith<ColorParseException> { ColorConvert.parse("#12345") }
        assertFailsWith<ColorParseException> { ColorConvert.parse("#gggggg") }
        assertFailsWith<ColorParseException> { ColorConvert.parse("verde") }
        assertFailsWith<ColorParseException> { ColorConvert.parse("rgb(300, 0, 0)") }
    }

    @Test
    fun toolReportsFailureWithMessage() {
        val out = ColorTool.run(ToolInput.Text("nada disso"))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("Cor inválida"))
    }
}

@OptIn(ExperimentalTime::class)
class TimestampToolTest {

    private val fixedNow = Instant.parse("2026-08-02T12:00:00Z")

    init {
        TimestampTool.now = { fixedNow }
        TimestampTool.zone = { TimeZone.UTC }
    }

    @AfterTest
    fun restore() {
        TimestampTool.now = { kotlin.time.Clock.System.now() }
        TimestampTool.zone = { TimeZone.currentSystemDefault() }
    }

    private fun rows(input: String) =
        ((TimestampTool.run(ToolInput.Text(input)) as ToolOutput.Success).body as ToolBody.Rows)
            .rows.associate { it.label to it.value }

    @Test
    fun convertsUnixSeconds() {
        val r = rows("1778580000")
        assertEquals("1778580000", r["Unix (s)"])
        assertEquals("1778580000000", r["Unix (ms)"])
        assertEquals("2026-05-12T10:00:00Z", r["ISO 8601"])
        assertEquals("Tue, 12 May 2026 10:00:00 GMT", r["UTC"])
    }

    @Test
    fun detectsMilliseconds() {
        assertEquals("1778580000", rows("1778580000000")["Unix (s)"])
    }

    @Test
    fun parsesIso8601() {
        assertEquals("1778580000", rows("2026-05-12T10:00:00Z")["Unix (s)"])
    }

    @Test
    fun describesRelativeTimeInPortuguese() {
        assertEquals("há 2 meses", rows("1778580000")["Relativo"])
        assertEquals("em 1 hora", rows("2026-08-02T13:00:00Z")["Relativo"])
    }

    @Test
    fun rejectsGarbage() {
        val out = TimestampTool.run(ToolInput.Text("ontem"))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("Não reconheci"))
    }
}

@OptIn(ExperimentalTime::class)
class CronToolTest {

    init {
        CronTool.now = { Instant.parse("2026-08-02T12:00:00Z") }
        CronTool.zone = { TimeZone.UTC }
    }

    @AfterTest
    fun restore() {
        CronTool.now = { kotlin.time.Clock.System.now() }
        CronTool.zone = { TimeZone.currentSystemDefault() }
    }

    private fun rows(expression: String) =
        ((CronTool.run(ToolInput.Text(expression)) as ToolOutput.Success).body as ToolBody.Rows)
            .rows.associate { it.label to it.value }

    @Test
    fun describesWeekdaysAtNine() {
        val r = rows("0 9 * * 1-5")
        assertEquals("Às 09:00 todos os dias úteis", r["Descrição"])
        assertEquals("1-5 (segunda a sexta)", r["Dia da semana"])
        assertEquals("2026-08-03 09:00", r["Próxima execução"])
    }

    @Test
    fun expandsStepsAndLists() {
        val cron = Cron.parse("*/15 0 1,15 * *")
        assertEquals(setOf(0, 15, 30, 45), cron.minute.values)
        assertEquals(setOf(1, 15), cron.dayOfMonth.values)
    }

    @Test
    fun describesEveryMinute() {
        assertEquals("A cada minuto todos os dias", rows("* * * * *")["Descrição"])
    }

    @Test
    fun rejectsWrongFieldCount() {
        val e = assertFailsWith<CronParseException> { Cron.parse("0 9 * *") }
        assertTrue(e.message!!.contains("5 campos"))
    }

    @Test
    fun rejectsOutOfRangeValues() {
        assertFailsWith<CronParseException> { Cron.parse("60 9 * * *") }
        assertFailsWith<CronParseException> { Cron.parse("0 25 * * *") }
        assertFailsWith<CronParseException> { Cron.parse("0 9 32 * *") }
    }

    @Test
    fun rejectsUnsupportedSyntax() {
        assertTrue(
            assertFailsWith<CronParseException> { Cron.parse("@daily") }.message!!.contains("atalhos"),
        )
        assertTrue(
            assertFailsWith<CronParseException> { Cron.parse("0 9 L * *") }.message!!.contains("L"),
        )
    }

    @Test
    fun toolReportsFailure() {
        val out = CronTool.run(ToolInput.Text("nada"))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("Cron inválido"))
    }
}

class QrCodeTest {

    @Test
    fun producesTheRightMatrixSizeForVersionOne() {
        val modules = QrCode.encode("https://devtoolbox.dev")
        assertEquals(25, modules.size)
        assertTrue(modules.all { it.size == modules.size })
    }

    @Test
    fun placesTheThreeFinderPatterns() {
        val m = QrCode.encode("OI")
        val size = m.size
        for ((row, col) in listOf(0 to 0, 0 to size - 7, size - 7 to 0)) {
            assertTrue(m[row][col], "canto ($row,$col) deveria ser escuro")
            assertTrue(m[row + 1][col + 1] == false, "anel claro faltando em ($row,$col)")
            assertTrue(m[row + 3][col + 3], "miolo faltando em ($row,$col)")
        }
    }

    @Test
    fun placesTimingPatterns() {
        val m = QrCode.encode("OI")
        for (i in 8 until m.size - 8) {
            assertEquals(i % 2 == 0, m[6][i], "timing horizontal em $i")
            assertEquals(i % 2 == 0, m[i][6], "timing vertical em $i")
        }
    }

    @Test
    fun formatInformationMatchesTheSpecTable() {
        val expected = listOf(0x5412, 0x5125, 0x5E7C, 0x5B4B, 0x45F9, 0x40CE, 0x4F97, 0x4AA0)
        assertEquals(expected, (0..7).map { QrCode.formatBits(it) })
    }

    @Test
    fun reedSolomonRemainderIsDivisibleByTheGenerator() {
        val data = IntArray(16) { (it * 37 + 11) and 0xFF }
        val ec = ReedSolomon.encode(data, 10)
        val codeword = data + ec
        val gen = ReedSolomon.generator(10)

        val remainder = codeword.copyOf()
        for (i in data.indices) {
            val factor = remainder[i]
            if (factor == 0) continue
            for (j in gen.indices) {
                remainder[i + j] = remainder[i + j] xor ReedSolomon.multiply(gen[j], factor)
            }
        }
        assertTrue(
            remainder.drop(data.size).all { it == 0 },
            "resto não nulo: ${remainder.drop(data.size)}",
        )
    }

    @Test
    fun growsVersionWithContentLength() {
        assertEquals(21, QrCode.encode("a".repeat(10)).size)
        assertEquals(25, QrCode.encode("a".repeat(20)).size)
        assertEquals(29, QrCode.encode("a".repeat(40)).size)
    }

    @Test
    fun rejectsContentThatIsTooLong() {
        val out = QrTool.run(ToolInput.Text("a".repeat(300)))
        assertIs<ToolOutput.Failure>(out)
        assertTrue(out.message.contains("longo demais"))
    }

    @Test
    fun rejectsEmptyContent() {
        assertIs<ToolOutput.Failure>(QrTool.run(ToolInput.Text("")))
    }

    @Test
    fun alignmentPatternSurvivesTheMask() {
        val m = QrCode.encode("https://devtoolbox.dev")
        for (dr in -2..2) {
            for (dc in -2..2) {
                val expected = dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0)
                assertEquals(expected, m[18 + dr][18 + dc], "alinhamento errado em ($dr,$dc)")
            }
        }
    }

    @Test
    fun bothFormatCopiesCarryAValidFormatString() {
        val valid = setOf(0x5412, 0x5125, 0x5E7C, 0x5B4B, 0x45F9, 0x40CE, 0x4F97, 0x4AA0)

        for (text in listOf("https://devtoolbox.dev", "OI", "a".repeat(40), "teste 123")) {
            val m = QrCode.encode(text)
            val size = m.size

            var first = 0
            var second = 0
            for (i in 0..14) {
                val firstBit = when {
                    i < 6 -> m[8][i]
                    i == 6 -> m[8][7]
                    i == 7 -> m[8][8]
                    i == 8 -> m[7][8]
                    else -> m[14 - i][8]
                }
                val secondBit = if (i < 7) m[size - 1 - i][8] else m[8][size - 8 + (i - 7)]
                if (firstBit) first = first or (1 shl (14 - i))
                if (secondBit) second = second or (1 shl (14 - i))
            }

            assertTrue(first in valid, "cópia 1 inválida para \"$text\": ${first.toString(16)}")
            assertTrue(second in valid, "cópia 2 inválida para \"$text\": ${second.toString(16)}")
            assertEquals(first, second, "as duas cópias divergem para \"$text\"")
        }
    }

    @Test
    fun darkModuleIsSet() {
        val m = QrCode.encode("OI")
        assertTrue(m[m.size - 8][8], "módulo escuro fixo ausente")
    }

    @Test
    fun dataModuleCountMatchesTheSpec() {
        val m = QrCode.encode("https://devtoolbox.dev")
        assertEquals(25, m.size)
        val functionModules = QrDecoder.functionModuleCount(m.size, version = 2)
        assertEquals(266, functionModules)
        assertEquals(44 * 8 + 7, m.size * m.size - functionModules)
    }

    @Test
    fun matrixDecodesBackToTheOriginalText() {
        for (text in listOf(
            "https://devtoolbox.dev",
            "OI",
            "a",
            "Ação — çãé 日本語",
            "https://exemplo.com/caminho/bem/mais/longo?com=parametros&e=coisas",
            "a".repeat(60),
        )) {
            assertEquals(text, QrDecoder.decode(QrCode.encode(text)), "falhou para: $text")
        }
    }

    @Test
    fun decodedTextSurvivesEveryVersionInRange() {
        for (length in listOf(1, 14, 15, 26, 27, 42, 43, 62, 84)) {
            val text = "x".repeat(length)
            assertEquals(text, QrDecoder.decode(QrCode.encode(text)), "falhou com $length bytes")
        }
    }
}

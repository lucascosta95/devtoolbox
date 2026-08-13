package dev.devtoolbox.ui

import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.ToolRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InputHostingTest {

    @Test
    fun aTokenWithTwoPartsFailsButTheUiStillDrawsTheBody() {
        val input = ToolInput.Text("abc.def")

        val output = ToolRegistry.byId("jwt")!!.run(input)

        assertIs<ToolOutput.Failure>(output)
        assertEquals("JWT deve ter 3 partes separadas por ponto — recebidas 2.", output.message)
        assertNull(output.body, "o core não devolve corpo — quem preenche a lacuna é a UI")

        val body = fallbackBody("jwt", input)
        assertIs<ToolBody.Jwt>(body)
        assertTrue(body.headerJson.isBlank(), "header vazio faz o painel mostrar o erro do core")
        assertTrue(body.payloadJson.isBlank(), "payload vazio faz o painel mostrar o erro do core")
    }

    @Test
    fun anEmptyTokenAlsoKeepsThePanelsOnScreen() {
        val input = ToolInput.Text("")

        val output = ToolRegistry.byId("jwt")!!.run(input)

        assertIs<ToolOutput.Failure>(output)
        assertIs<ToolBody.Jwt>(fallbackBody("jwt", input))
    }

    @Test
    fun anInvalidPatternKeepsTheTypedSubject() {
        val input = ToolInput.Pattern(pattern = "(", flags = "g", subject = "Suporte: 555-0132.")

        val output = ToolRegistry.byId("regex")!!.run(input)

        assertIs<ToolOutput.Failure>(output)
        assertTrue(output.message.startsWith("Regex inválida:"))

        val body = fallbackBody("regex", input)
        assertIs<ToolBody.Regex>(body)
        assertEquals("Suporte: 555-0132.", body.segments.joinToString("") { it.text })
        assertTrue(body.matches.isEmpty())
        assertTrue(body.segments.none { it.matched })
    }

    @Test
    fun anEmptyPatternKeepsTheTypedSubjectToo() {
        val input = ToolInput.Pattern(pattern = "", flags = "", subject = "continua aqui")

        assertIs<ToolOutput.Failure>(ToolRegistry.byId("regex")!!.run(input))

        val body = fallbackBody("regex", input)
        assertIs<ToolBody.Regex>(body)
        assertEquals("continua aqui", body.segments.joinToString("") { it.text })
    }

    @Test
    fun toolsThatDoNotHostTheirInputHaveNoFallback() {
        assertNull(fallbackBody("base64", ToolInput.Text("abc")))
        assertNull(fallbackBody("cron", ToolInput.Text("0 9 L * *")))
        assertNull(fallbackBody("jwt", ToolInput.Seed(0)))
    }
}

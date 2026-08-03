package dev.devtoolbox.core.util

import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Conversão entre HEX, RGB, HSL e OKLCH.
 *
 * OKLCH usa o caminho canônico sRGB → linear → LMS → OKLab → OKLCH, com as matrizes de
 * Björn Ottosson. Os valores são conferidos por round-trip nos testes.
 */
data class Rgb(val r: Int, val g: Int, val b: Int)

class ColorParseException(message: String) : Exception(message)

object ColorConvert {

    // ------------------------------------------------------------- parsing

    /** Aceita `#rgb`, `#rrggbb`, `rgb(r, g, b)`, `hsl(h, s%, l%)` e `oklch(l% c h)`. */
    fun parse(text: String): Rgb {
        val t = text.trim().lowercase()
        return when {
            t.startsWith("#") -> parseHex(t)
            t.startsWith("rgb") -> parseRgb(t)
            t.startsWith("hsl") -> parseHsl(t)
            t.startsWith("oklch") -> parseOklch(t)
            Regex("^[0-9a-f]{3}$|^[0-9a-f]{6}$").matches(t) -> parseHex("#$t")
            else -> throw ColorParseException(
                "não reconheci a cor — use #hex, rgb(), hsl() ou oklch().",
            )
        }
    }

    private fun parseHex(text: String): Rgb {
        val hex = text.removePrefix("#")
        val full = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> throw ColorParseException("HEX precisa ter 3 ou 6 dígitos.")
        }
        val value = full.toIntOrNull(16)
            ?: throw ColorParseException("HEX contém caractere não hexadecimal.")
        return Rgb(value shr 16 and 0xFF, value shr 8 and 0xFF, value and 0xFF)
    }

    private fun numbers(text: String): List<Double> =
        Regex("-?\\d+(?:\\.\\d+)?").findAll(text).map { it.value.toDouble() }.toList()

    private fun parseRgb(text: String): Rgb {
        val n = numbers(text)
        if (n.size < 3) throw ColorParseException("rgb() precisa de três componentes.")
        if (n.take(3).any { it < 0 || it > 255 }) {
            throw ColorParseException("componentes de rgb() vão de 0 a 255.")
        }
        return Rgb(n[0].roundToInt(), n[1].roundToInt(), n[2].roundToInt())
    }

    private fun parseHsl(text: String): Rgb {
        val n = numbers(text)
        if (n.size < 3) throw ColorParseException("hsl() precisa de três componentes.")
        return hslToRgb(n[0], n[1] / 100.0, n[2] / 100.0)
    }

    private fun parseOklch(text: String): Rgb {
        val n = numbers(text)
        if (n.size < 3) throw ColorParseException("oklch() precisa de três componentes.")
        // O primeiro valor pode vir como 68.4% ou 0.684.
        val l = if (text.contains('%')) n[0] / 100.0 else n[0]
        return oklchToRgb(l, n[1], n[2])
    }

    // --------------------------------------------------------- formatação

    fun toHex(c: Rgb): String =
        "#" + listOf(c.r, c.g, c.b).joinToString("") { it.coerceIn(0, 255).toString(16).padStart(2, '0') }

    fun toRgbString(c: Rgb): String = "rgb(${c.r}, ${c.g}, ${c.b})"

    fun toHslString(c: Rgb): String {
        val (h, s, l) = rgbToHsl(c)
        return "hsl(${h.roundToInt()}, ${(s * 100).roundToInt()}%, ${(l * 100).roundToInt()}%)"
    }

    fun toOklchString(c: Rgb): String {
        val (l, chroma, hue) = rgbToOklch(c)
        return "oklch(${round(l * 100, 1)}% ${round(chroma, 3)} ${round(hue, 1)})"
    }

    private fun round(value: Double, decimals: Int): String {
        val factor = 10.0.pow(decimals)
        val rounded = (value * factor).roundToInt() / factor
        return if (decimals == 0) rounded.roundToInt().toString() else rounded.toString()
    }

    // ---------------------------------------------------------------- HSL

    fun rgbToHsl(c: Rgb): Triple<Double, Double, Double> {
        val r = c.r / 255.0
        val g = c.g / 255.0
        val b = c.b / 255.0
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val l = (max + min) / 2

        if (delta == 0.0) return Triple(0.0, 0.0, l)

        val s = delta / (1 - abs(2 * l - 1))
        val h = when (max) {
            r -> 60 * (((g - b) / delta) % 6)
            g -> 60 * ((b - r) / delta + 2)
            else -> 60 * ((r - g) / delta + 4)
        }
        return Triple(if (h < 0) h + 360 else h, s, l)
    }

    fun hslToRgb(hDeg: Double, s: Double, l: Double): Rgb {
        val h = ((hDeg % 360) + 360) % 360
        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs((h / 60) % 2 - 1))
        val m = l - c / 2
        val (r1, g1, b1) = when {
            h < 60 -> Triple(c, x, 0.0)
            h < 120 -> Triple(x, c, 0.0)
            h < 180 -> Triple(0.0, c, x)
            h < 240 -> Triple(0.0, x, c)
            h < 300 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        return Rgb(
            ((r1 + m) * 255).roundToInt().coerceIn(0, 255),
            ((g1 + m) * 255).roundToInt().coerceIn(0, 255),
            ((b1 + m) * 255).roundToInt().coerceIn(0, 255),
        )
    }

    // -------------------------------------------------------------- OKLCH

    private fun srgbToLinear(v: Double): Double =
        if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

    private fun linearToSrgb(v: Double): Double =
        if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1 / 2.4) - 0.055

    /** Devolve (L, C, H) com L em 0..1, C em torno de 0..0.4 e H em graus. */
    fun rgbToOklch(c: Rgb): Triple<Double, Double, Double> {
        val r = srgbToLinear(c.r / 255.0)
        val g = srgbToLinear(c.g / 255.0)
        val b = srgbToLinear(c.b / 255.0)

        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)

        val okL = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val okA = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val okB = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s

        val chroma = kotlin.math.sqrt(okA * okA + okB * okB)
        var hue = kotlin.math.atan2(okB, okA) * 180.0 / kotlin.math.PI
        if (hue < 0) hue += 360
        return Triple(okL, chroma, hue)
    }

    fun oklchToRgb(okL: Double, chroma: Double, hueDeg: Double): Rgb {
        val hue = hueDeg * kotlin.math.PI / 180.0
        val okA = chroma * kotlin.math.cos(hue)
        val okB = chroma * kotlin.math.sin(hue)

        val l = (okL + 0.3963377774 * okA + 0.2158037573 * okB).pow(3)
        val m = (okL - 0.1055613458 * okA - 0.0638541728 * okB).pow(3)
        val s = (okL - 0.0894841775 * okA - 1.2914855480 * okB).pow(3)

        val r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
        val g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
        val b = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

        return Rgb(
            (linearToSrgb(r) * 255).roundToInt().coerceIn(0, 255),
            (linearToSrgb(g) * 255).roundToInt().coerceIn(0, 255),
            (linearToSrgb(b) * 255).roundToInt().coerceIn(0, 255),
        )
    }
}

package dev.devtoolbox.ds

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

object Oklab {
    fun mix(a: Color, b: Color, ratio: Float): Color {
        val t = ratio.coerceIn(0f, 1f)
        val (l1, a1, b1) = toOklab(a)
        val (l2, a2, b2) = toOklab(b)
        return fromOklab(
            l = l1 * t + l2 * (1 - t),
            aAxis = a1 * t + a2 * (1 - t),
            bAxis = b1 * t + b2 * (1 - t),
        ).copy(alpha = a.alpha * t + b.alpha * (1 - t))
    }

    private fun toOklab(color: Color): Triple<Float, Float, Float> {
        val r = toLinear(color.red)
        val g = toLinear(color.green)
        val b = toLinear(color.blue)

        val l = (0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b).cbrt()
        val m = (0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b).cbrt()
        val s = (0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b).cbrt()

        return Triple(
            0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
            1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
            0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
        )
    }

    private fun fromOklab(l: Float, aAxis: Float, bAxis: Float): Color {
        val lc = (l + 0.3963377774f * aAxis + 0.2158037573f * bAxis).let { it * it * it }
        val mc = (l - 0.1055613458f * aAxis - 0.0638541728f * bAxis).let { it * it * it }
        val sc = (l - 0.0894841775f * aAxis - 1.2914855480f * bAxis).let { it * it * it }

        return Color(
            red = fromLinear(4.0767416621f * lc - 3.3077115913f * mc + 0.2309699292f * sc),
            green = fromLinear(-1.2684380046f * lc + 2.6097574011f * mc - 0.3413193965f * sc),
            blue = fromLinear(-0.0041960863f * lc - 0.7034186147f * mc + 1.7076147010f * sc),
        )
    }

    private fun toLinear(channel: Float): Float =
        if (channel <= 0.04045f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)

    private fun fromLinear(channel: Float): Float {
        val v = if (channel <= 0.0031308f) channel * 12.92f else 1.055f * channel.pow(1f / 2.4f) - 0.055f
        return v.coerceIn(0f, 1f)
    }

    private fun Float.cbrt(): Float =
        if (this < 0f) -((-this).pow(1f / 3f)) else this.pow(1f / 3f)
}

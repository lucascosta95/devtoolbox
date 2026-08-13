package dev.devtoolbox.core.util

object CodePoints {

    fun split(text: String): List<String> {
        val points = ArrayList<String>(text.length)
        var i = 0
        while (i < text.length) {
            val high = text[i]
            val paired = high.isHighSurrogate() &&
                i + 1 < text.length &&
                text[i + 1].isLowSurrogate()
            val size = if (paired) 2 else 1
            points += text.substring(i, i + size)
            i += size
        }
        return points
    }

    fun count(text: String): Int = split(text).size
}

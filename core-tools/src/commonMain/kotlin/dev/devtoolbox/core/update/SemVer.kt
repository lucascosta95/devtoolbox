package dev.devtoolbox.core.update

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String = "",
) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        val core = compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)
        if (core != 0) return core
        return when {
            preRelease == other.preRelease -> 0
            preRelease.isEmpty() -> 1
            other.preRelease.isEmpty() -> -1
            else -> preRelease.compareTo(other.preRelease)
        }
    }

    override fun toString(): String =
        "$major.$minor.$patch" + if (preRelease.isEmpty()) "" else "-$preRelease"

    companion object {
        fun parse(text: String): SemVer? {
            val trimmed = text.trim().removePrefix("v").removePrefix("V")
            if (trimmed.isEmpty()) return null

            val withoutBuild = trimmed.substringBefore('+')
            val core = withoutBuild.substringBefore('-')
            val preRelease = withoutBuild.substringAfter('-', "")

            val parts = core.split('.')
            if (parts.size > 3) return null

            val numbers = parts.map { part ->
                val number = part.toIntOrNull() ?: return null
                if (number < 0) return null
                number
            }

            return SemVer(
                major = numbers[0],
                minor = numbers.getOrElse(1) { 0 },
                patch = numbers.getOrElse(2) { 0 },
                preRelease = preRelease,
            )
        }
    }
}

package dev.devtoolbox.core.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Release(
    @SerialName("tag_name") val tag: String = "",
    @SerialName("html_url") val url: String = "",
    val name: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
) {
    val version: SemVer? get() = SemVer.parse(tag)
}

object ReleaseCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(text: String): Release? = runCatching { json.decodeFromString<Release>(text) }
        .getOrNull()
        ?.takeIf { it.tag.isNotBlank() }
}

object GitHubReleases {

    fun latestApiUrl(repo: String): String = "https://api.github.com/repos/$repo/releases/latest"

    fun latestPageUrl(repo: String): String = "https://github.com/$repo/releases/latest"
}

package dev.devtoolbox.ui

import dev.devtoolbox.core.BuildInfo
import dev.devtoolbox.core.update.GitHubReleases
import dev.devtoolbox.core.update.Release
import dev.devtoolbox.core.update.ReleaseCodec
import dev.devtoolbox.core.update.ReleaseFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(10)

private const val HTTP_OK = 200

class GitHubReleaseFetcher(private val repo: String = BuildInfo.REPO) : ReleaseFetcher {

    private val client: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    override suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val request = HttpRequest.newBuilder(URI.create(GitHubReleases.latestApiUrl(repo)))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "DevToolbox/${BuildInfo.VERSION}")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == HTTP_OK) ReleaseCodec.decode(response.body()) else null
        }.getOrNull()
    }
}

actual fun createReleaseFetcher(): ReleaseFetcher = GitHubReleaseFetcher()

actual fun openInBrowser(url: String) {
    val browsed = runCatching {
        val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
        if (desktop?.isSupported(Desktop.Action.BROWSE) == true) {
            desktop.browse(URI.create(url))
            true
        } else {
            false
        }
    }.getOrDefault(false)

    if (!browsed) runCatching { ProcessBuilder(openCommand(url)).start() }
}

private fun openCommand(url: String): List<String> {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        os.contains("mac") || os.contains("darwin") -> listOf("open", url)
        os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
        else -> listOf("xdg-open", url)
    }
}

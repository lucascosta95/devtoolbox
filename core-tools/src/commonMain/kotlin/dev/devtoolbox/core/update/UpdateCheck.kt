package dev.devtoolbox.core.update

fun interface ReleaseFetcher {
    suspend fun latest(): Release?
}

object NoOpReleaseFetcher : ReleaseFetcher {
    override suspend fun latest(): Release? = null
}

fun pendingUpdate(current: String, release: Release?, skipped: String? = null): Release? {
    if (release == null) return null
    if (release.draft || release.prerelease) return null

    val candidate = release.version ?: return null
    if (candidate.preRelease.isNotEmpty()) return null

    val installed = SemVer.parse(current) ?: return null
    if (candidate <= installed) return null

    val ignored = skipped?.let(SemVer::parse)
    if (ignored != null && candidate <= ignored) return null

    return release
}

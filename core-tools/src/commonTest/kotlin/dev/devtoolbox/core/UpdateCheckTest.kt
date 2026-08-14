package dev.devtoolbox.core

import dev.devtoolbox.core.update.GitHubReleases
import dev.devtoolbox.core.update.Release
import dev.devtoolbox.core.update.ReleaseCodec
import dev.devtoolbox.core.update.SemVer
import dev.devtoolbox.core.update.pendingUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val CURRENT = "1.4.0"

class UpdateCheckTest {

    @Test
    fun parsesTagsWithAndWithoutThePrefix() {
        assertEquals(SemVer(1, 4, 0), SemVer.parse("1.4.0"))
        assertEquals(SemVer(1, 4, 0), SemVer.parse("v1.4.0"))
        assertEquals(SemVer(1, 4, 0), SemVer.parse(" v1.4.0 "))
        assertEquals(SemVer(2, 0, 0), SemVer.parse("v2"))
        assertEquals(SemVer(2, 1, 0), SemVer.parse("v2.1"))
    }

    @Test
    fun comparesNumericallyAndNotAsText() {
        val older = SemVer.parse("v1.9.0")!!
        val newer = SemVer.parse("v1.10.0")!!
        assertTrue(newer > older, "1.10.0 precisa ser maior que 1.9.0")
        assertTrue(SemVer.parse("v2.0.0")!! > newer)
        assertEquals(0, SemVer.parse("1.4.0")!!.compareTo(SemVer.parse("v1.4.0")!!))
    }

    @Test
    fun aPreReleaseIsOlderThanTheSameFinalVersion() {
        val candidate = SemVer.parse("v1.5.0-rc1")!!
        assertEquals("rc1", candidate.preRelease)
        assertTrue(candidate < SemVer.parse("1.5.0")!!)
    }

    @Test
    fun garbageTagsDoNotParse() {
        assertNull(SemVer.parse(""))
        assertNull(SemVer.parse("latest"))
        assertNull(SemVer.parse("v1.x.0"))
        assertNull(SemVer.parse("1.2.3.4"))
    }

    @Test
    fun decodesTheGithubPayloadIgnoringTheExtraKeys() {
        val payload = """
            {
              "url": "https://api.github.com/repos/lucascosta95/devtoolbox/releases/1",
              "html_url": "https://github.com/lucascosta95/devtoolbox/releases/tag/v1.5.0",
              "id": 1,
              "tag_name": "v1.5.0",
              "name": "v1.5.0",
              "draft": false,
              "prerelease": false,
              "assets": [{ "name": "devtoolbox_1.5.0_amd64.deb" }],
              "body": "notas da release"
            }
        """.trimIndent()

        val release = ReleaseCodec.decode(payload)
        assertNotNull(release)
        assertEquals("v1.5.0", release.tag)
        assertEquals("https://github.com/lucascosta95/devtoolbox/releases/tag/v1.5.0", release.url)
        assertEquals(SemVer(1, 5, 0), release.version)
    }

    @Test
    fun brokenPayloadsDecodeToNullInsteadOfThrowing() {
        assertNull(ReleaseCodec.decode(""))
        assertNull(ReleaseCodec.decode("{ isso não é json"))
        assertNull(ReleaseCodec.decode("{}"))
        assertNull(ReleaseCodec.decode("""{"message":"Not Found"}"""))
    }

    @Test
    fun aNewerReleaseIsOffered() {
        val release = Release(tag = "v1.5.0", url = "https://github.com/x/y/releases/tag/v1.5.0")
        assertEquals(release, pendingUpdate(CURRENT, release))
    }

    @Test
    fun theSameOrAnOlderReleaseIsNotOffered() {
        assertNull(pendingUpdate(CURRENT, Release(tag = "v1.4.0")))
        assertNull(pendingUpdate(CURRENT, Release(tag = "v1.3.9")))
    }

    @Test
    fun draftsAndPreReleasesAreNotOffered() {
        assertNull(pendingUpdate(CURRENT, Release(tag = "v1.5.0", draft = true)))
        assertNull(pendingUpdate(CURRENT, Release(tag = "v1.5.0", prerelease = true)))
        assertNull(pendingUpdate(CURRENT, Release(tag = "v1.5.0-rc1")))
    }

    @Test
    fun nothingIsOfferedWhenTheFetchFailed() {
        assertNull(pendingUpdate(CURRENT, release = null))
    }

    @Test
    fun aSkippedVersionIsNotOfferedAgain() {
        assertNull(pendingUpdate(CURRENT, Release(tag = "v1.5.0"), skipped = "1.5.0"))
        assertNull(pendingUpdate(CURRENT, Release(tag = "1.5.0"), skipped = "v1.5.0"))
    }

    @Test
    fun aVersionNewerThanTheSkippedOneIsOfferedAgain() {
        val release = Release(tag = "v1.6.0")
        assertEquals(release, pendingUpdate(CURRENT, release, skipped = "1.5.0"))
    }

    @Test
    fun anUnparseableSkipMarkerDoesNotSwallowTheUpdate() {
        val release = Release(tag = "v1.5.0")
        assertEquals(release, pendingUpdate(CURRENT, release, skipped = "lixo"))
    }

    @Test
    fun unparseableVersionsNeverTriggerTheDialog() {
        assertNull(pendingUpdate(CURRENT, Release(tag = "nightly")))
        assertNull(pendingUpdate("dev", Release(tag = "v1.5.0")))
    }

    @Test
    fun buildsTheGithubUrlsFromTheRepo() {
        assertEquals(
            "https://api.github.com/repos/dono/app/releases/latest",
            GitHubReleases.latestApiUrl("dono/app"),
        )
        assertEquals(
            "https://github.com/dono/app/releases/latest",
            GitHubReleases.latestPageUrl("dono/app"),
        )
    }
}

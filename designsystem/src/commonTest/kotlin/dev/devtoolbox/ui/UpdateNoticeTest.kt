package dev.devtoolbox.ui

import dev.devtoolbox.core.BuildInfo
import dev.devtoolbox.core.persistence.InMemoryStateStore
import dev.devtoolbox.core.persistence.PersistedState
import dev.devtoolbox.core.persistence.StateCodec
import dev.devtoolbox.core.update.Release
import dev.devtoolbox.core.update.ReleaseFetcher
import dev.devtoolbox.core.update.SemVer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private const val RELEASE_URL = "https://github.com/lucascosta95/devtoolbox/releases/tag/next"

private fun newerRelease(bump: Int = 1): Release {
    val installed = SemVer.parse(BuildInfo.VERSION)!!
    return Release(tag = "v${installed.major + bump}.0.0", url = RELEASE_URL)
}

private fun fetcherOf(release: Release?) = ReleaseFetcher { release }

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateNoticeTest {

    @Test
    fun aNewerReleaseOpensTheDialogOnStartup() = runTest {
        val release = newerRelease()
        val vm = viewModel(backgroundScope, fetcherOf(release))

        vm.checkForUpdates()
        runCurrent()

        val notice = assertIs<UpdateNotice.Available>(vm.state.value.updateNotice)
        assertEquals(release, notice.release)
    }

    @Test
    fun theStartupCheckSaysNothingWhenTheAppIsUpToDate() = runTest {
        val vm = viewModel(backgroundScope, fetcherOf(Release(tag = "v${BuildInfo.VERSION}", url = RELEASE_URL)))

        vm.checkForUpdates()
        runCurrent()

        assertNull(vm.state.value.updateNotice)
    }

    @Test
    fun aFetcherThatFailsNeverShowsAnythingAndDoesNotBreakTheScope() = runTest {
        val vm = viewModel(backgroundScope, ReleaseFetcher { error("sem rede") })

        vm.checkForUpdates()
        runCurrent()

        assertNull(vm.state.value.updateNotice)
    }

    @Test
    fun remindLaterKeepsNothingAndTheDialogComesBackOnTheNextLaunch() = runTest {
        val store = InMemoryStateStore()
        val fetcher = fetcherOf(newerRelease())

        val first = viewModel(backgroundScope, fetcher, store)
        first.checkForUpdates()
        runCurrent()
        first.dismissUpdate()
        advanceTimeBy(PERSIST_DEBOUNCE_MS + 50)

        assertNull(first.state.value.updateNotice)
        assertNull(store.load()!!.skippedVersion, "lembrar depois não pode marcar a versão como pulada")

        val next = viewModel(backgroundScope, fetcher, store)
        next.checkForUpdates()
        runCurrent()
        assertIs<UpdateNotice.Available>(next.state.value.updateNotice)
    }

    @Test
    fun aSkippedVersionIsPersistedAndStaysQuietOnTheNextLaunch() = runTest {
        val store = InMemoryStateStore()
        val skipped = newerRelease()

        val first = viewModel(backgroundScope, fetcherOf(skipped), store)
        first.checkForUpdates()
        runCurrent()
        first.skipVersion(skipped)
        advanceTimeBy(PERSIST_DEBOUNCE_MS + 50)

        assertNull(first.state.value.updateNotice)
        assertEquals(skipped.version.toString(), store.load()!!.skippedVersion)

        val next = viewModel(backgroundScope, fetcherOf(skipped), store)
        assertEquals(skipped.version.toString(), next.state.value.skippedVersion, "a escolha precisa voltar do disco")
        next.checkForUpdates()
        runCurrent()
        assertNull(next.state.value.updateNotice, "a versão pulada não pode voltar a incomodar")
    }

    @Test
    fun aVersionNewerThanTheSkippedOneStillWarns() = runTest {
        val store = InMemoryStateStore()
        val skipped = newerRelease()

        val first = viewModel(backgroundScope, fetcherOf(skipped), store)
        first.checkForUpdates()
        runCurrent()
        first.skipVersion(skipped)
        advanceTimeBy(PERSIST_DEBOUNCE_MS + 50)

        val later = newerRelease(bump = 2)
        val next = viewModel(backgroundScope, fetcherOf(later), store)
        next.checkForUpdates()
        runCurrent()

        val notice = assertIs<UpdateNotice.Available>(next.state.value.updateNotice)
        assertEquals(later, notice.release, "uma release posterior à pulada precisa avisar de novo")
    }

    @Test
    fun theManualCheckReportsThatTheAppIsUpToDateAndClearsTheSkip() = runTest {
        val store = InMemoryStateStore(PersistedState(skippedVersion = "9.9.9"))
        val vm = viewModel(backgroundScope, fetcherOf(null), store)

        vm.checkForUpdatesManually()
        advanceTimeBy(PERSIST_DEBOUNCE_MS + 50)

        assertEquals(UpdateNotice.UpToDate, vm.state.value.updateNotice)
        assertNull(store.load()!!.skippedVersion)
    }

    @Test
    fun openingTheReleaseUsesTheUrlFromGithubAndClosesTheDialog() = runTest {
        val opened = mutableListOf<String>()
        val release = newerRelease()
        val vm = AppViewModel(
            scope = backgroundScope,
            store = InMemoryStateStore(),
            releases = fetcherOf(release),
            openUrl = opened::add,
        )

        vm.checkForUpdates()
        runCurrent()
        vm.openRelease(release)

        assertEquals(listOf(RELEASE_URL), opened)
        assertNull(vm.state.value.updateNotice)
    }

    @Test
    fun aReleaseWithoutAUrlFallsBackToTheReleasesPage() = runTest {
        val opened = mutableListOf<String>()
        val vm = AppViewModel(scope = backgroundScope, openUrl = opened::add)

        vm.openRelease(Release(tag = "v9.0.0"))

        assertEquals(listOf("https://github.com/${BuildInfo.REPO}/releases/latest"), opened)
    }

    @Test
    fun theStoredPreferenceSurvivesARoundTripThroughJson() {
        val encoded = StateCodec.encode(PersistedState(skippedVersion = "1.5.0"))
        assertEquals("1.5.0", StateCodec.decode(encoded)!!.skippedVersion)
    }

    @Test
    fun aStateFileFromBeforeTheUpdateCheckStillLoads() {
        val legacy = """
            {
              "selected_id": "cron",
              "favorites": ["base64"],
              "theme": "light",
              "accent": "teal",
              "version": 1
            }
        """.trimIndent()

        val decoded = StateCodec.decode(legacy)!!
        assertNull(decoded.skippedVersion)
        assertNull(AppState().mergedWith(decoded).skippedVersion)
    }

    private fun viewModel(
        scope: CoroutineScope,
        fetcher: ReleaseFetcher,
        store: InMemoryStateStore = InMemoryStateStore(),
    ) = AppViewModel(
        scope = scope,
        store = store,
        releases = fetcher,
        openUrl = {},
    )
}

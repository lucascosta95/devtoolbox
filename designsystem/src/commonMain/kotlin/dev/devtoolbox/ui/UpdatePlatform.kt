package dev.devtoolbox.ui

import dev.devtoolbox.core.update.ReleaseFetcher

expect fun createReleaseFetcher(): ReleaseFetcher

expect fun openInBrowser(url: String)

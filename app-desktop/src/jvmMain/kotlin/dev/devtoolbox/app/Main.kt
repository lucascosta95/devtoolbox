package dev.devtoolbox.app

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.devtoolbox.ui.App
import dev.devtoolbox.ui.createReleaseFetcher
import dev.devtoolbox.ui.createStateStore

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1180.dp, 740.dp))
    val store = remember { createStateStore() }
    val releases = remember { createReleaseFetcher() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "DevToolbox",
        state = windowState,
    ) {
        window.minimumSize = java.awt.Dimension(1000, 640)
        App(store = store, releases = releases)
    }
}

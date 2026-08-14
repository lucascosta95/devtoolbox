package dev.devtoolbox.ds

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.ToolRegistry
import dev.devtoolbox.ds.components.PhosphorIcons
import kotlin.test.Test
import kotlin.test.assertTrue

class IconCoverageTest {

    @Test
    fun everyToolIconIsMapped() {
        val missing = ToolRegistry.all.map { it.icon }.filterNot { it in PhosphorIcons.names }
        assertTrue(missing.isEmpty(), "ícones de ferramenta sem recurso: $missing")
    }

    @Test
    fun everyCategoryIconIsMapped() {
        val missing = Category.entries.map { it.icon }.filterNot { it in PhosphorIcons.names }
        assertTrue(missing.isEmpty(), "ícones de categoria sem recurso: $missing")
    }

    @Test
    fun chromeIconsAreMapped() {
        val chrome = listOf(
            "wrench", "magnifying-glass", "star", "arrows-out-line-vertical",
            "sun", "moon", "arrow-right", "check", "copy", "x-circle", "drop-half", "image-square",
            "arrows-clockwise", "check-circle",
        )
        val missing = chrome.filterNot { it in PhosphorIcons.names }
        assertTrue(missing.isEmpty(), "ícones da moldura sem recurso: $missing")
    }
}

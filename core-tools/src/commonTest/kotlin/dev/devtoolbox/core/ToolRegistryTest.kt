package dev.devtoolbox.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToolRegistryTest {

    @Test
    fun idsAreUnique() {
        val ids = ToolRegistry.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "ids duplicados em ToolRegistry.all")
    }

    @Test
    fun everyToolHasCopy() {
        for (tool in ToolRegistry.all) {
            assertTrue(tool.name.isNotBlank(), "${tool.id} sem nome")
            assertTrue(tool.description.isNotBlank(), "${tool.id} sem descrição")
            assertTrue(tool.icon.isNotBlank(), "${tool.id} sem ícone")
        }
    }

    @Test
    fun everyToolRunsItsDefaultInputWithoutThrowing() {
        for (tool in ToolRegistry.all) {
            val out = tool.run(tool.defaultInput)
            assertTrue(out is ToolOutput.Success, "${tool.id} falhou com a própria entrada padrão")
        }
    }

    @Test
    fun emptyQueryReturnsEverything() {
        assertEquals(ToolRegistry.all, ToolRegistry.search(""))
        assertEquals(ToolRegistry.all, ToolRegistry.search("   "))
    }

    @Test
    fun searchesByName() {
        assertEquals(listOf("base64"), ToolRegistry.search("base64").map { it.id })
    }

    @Test
    fun searchIsCaseInsensitive() {
        assertEquals(ToolRegistry.search("json").map { it.id }, ToolRegistry.search("JSON").map { it.id })
    }

    @Test
    fun searchesByCategory() {
        val hits = ToolRegistry.search("encoding")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.category == Category.Encoding })
    }

    @Test
    fun searchMatchesPartialNames() {
        assertTrue(ToolRegistry.search("form").any { it.id == "json" })
    }

    @Test
    fun searchWithoutHitsReturnsEmpty() {
        assertTrue(ToolRegistry.search("zzzznaoexiste").isEmpty())
    }

    @Test
    fun categorizedDropsEmptyCategories() {
        val cats = ToolRegistry.categorized("encoding")
        assertEquals(listOf(Category.Encoding), cats.map { it.first })
    }

    @Test
    fun byIdFindsAndMisses() {
        assertNotNull(ToolRegistry.byId("base64"))
        assertNull(ToolRegistry.byId("naoexiste"))
    }

    @Test
    fun defaultToolIsBase64() {
        assertEquals("base64", ToolRegistry.default.id)
    }
}

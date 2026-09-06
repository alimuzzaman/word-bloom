package com.ritik.wordpuzzle.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogVersionTest {

    @Test
    fun `numeric release versions compare by each component`() {
        assertTrue(compareCatalogVersions("2026.09.02-2", "2026.09.02-1") > 0)
        assertTrue(compareCatalogVersions("2026.10.01-1", "2026.09.30-9") > 0)
        assertTrue(compareCatalogVersions("1.9", "1.10") < 0)
        assertEquals(0, compareCatalogVersions("2.0", "2.0.0"))
    }

    @Test
    fun `hash derived migration versions do not block a fresh schema one response`() {
        val first = "sha256-" + "a".repeat(64)
        val second = "sha256-" + "f".repeat(64)
        assertEquals(0, compareCatalogVersions(first, second))
        assertTrue(compareCatalogVersions("2.0", first) > 0)
    }
}

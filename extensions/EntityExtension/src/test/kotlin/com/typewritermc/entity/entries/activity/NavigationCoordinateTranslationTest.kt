package com.typewritermc.entity.entries.activity

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationCoordinateTranslationTest {
    @Test
    fun `physical and pathfinding coordinates round trip`() {
        listOf(-64.0, -40.5, 0.0, 1.0, 95.0, 319.0).forEach { physicalY ->
            assertEquals(physicalY, physicalY.toPathfindingY().toPhysicalY())
        }
    }

    @Test
    fun `pathfinding space is shifted above the legacy floor`() {
        assertEquals(0.0, (-64.0).toPathfindingY())
        assertEquals(65.0, 1.0.toPathfindingY())
    }
}

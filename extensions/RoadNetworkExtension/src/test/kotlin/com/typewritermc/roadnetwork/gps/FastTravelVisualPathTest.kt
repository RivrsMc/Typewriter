package com.typewritermc.roadnetwork.gps

import com.typewritermc.core.utils.point.Position
import com.typewritermc.core.utils.point.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FastTravelVisualPathTest {
    private val world = World("world")

    @Test
    fun `fast travel edge is rendered from its exact start to its exact end`() {
        val edge = GPSEdge(
            Position(world, 0.0, -40.0, 0.0),
            Position(world, 3.0, -39.0, 4.0),
            weight = 0.0,
            length = 0.0,
        )

        val path = edge.fastTravelVisualPath(spacing = 1.0)

        assertEquals(edge.start, path.first())
        assertEquals(edge.end, path.last())
        assertTrue(path.size > 2)
    }

    @Test
    fun `ordinary and cross-world edges do not get a synthetic visual bridge`() {
        val start = Position(world, 0.0, 0.0, 0.0)
        val end = Position(world, 2.0, 0.0, 0.0)
        val otherWorldEnd = Position(World("other"), 2.0, 0.0, 0.0)

        assertTrue(GPSEdge(start, end, weight = 2.0, length = 2.0).fastTravelVisualPath().isEmpty())
        assertTrue(GPSEdge(start, otherWorldEnd, weight = 0.0, length = 0.0).fastTravelVisualPath().isEmpty())
    }
}

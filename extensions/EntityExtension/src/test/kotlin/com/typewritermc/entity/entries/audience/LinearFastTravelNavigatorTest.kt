package com.typewritermc.entity.entries.audience

import com.typewritermc.core.utils.point.Position
import com.typewritermc.core.utils.point.World
import com.typewritermc.roadnetwork.gps.GPSEdge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinearFastTravelNavigatorTest {
    @Test
    fun `navigator crosses a gap linearly and completes at the destination`() {
        val edge = GPSEdge(
            Position(World("world"), 0.0, -40.0, 0.0),
            Position(World("world"), 4.0, -39.0, 0.0),
            weight = 0.0,
            length = 0.0,
        )
        val navigator = LinearFastTravelNavigator(edge, speed = 1.0)

        assertFalse(navigator.isComplete())
        repeat(10) { navigator.tick() }

        assertTrue(navigator.isComplete())
        assertEquals(edge.end.x, navigator.position().x)
        assertEquals(edge.end.y, navigator.position().y)
        assertEquals(edge.end.z, navigator.position().z)
    }
}

package com.typewritermc.entity.entries.activity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WalkingSteeringTest {
    @Test
    fun `a target underneath keeps yaw and levels pitch`() {
        var pitch = 60f
        val pitchVelocity = Velocity(0f)
        var yaw = 42f

        repeat(40) {
            val rotation = walkingLookRotation(
                LookDirection(yaw, pitch),
                0.1,
                -1.0,
                0.0,
                Velocity(0f),
                pitchVelocity,
            )
            yaw = rotation.first
            pitch = rotation.second
        }

        assertEquals(42f, yaw)
        assertTrue(kotlin.math.abs(pitch) < 2f)
    }

    @Test
    fun `a target ahead turns yaw while keeping pitch level`() {
        var yaw = 0f
        var pitch = 0f
        val yawVelocity = Velocity(0f)
        val pitchVelocity = Velocity(0f)

        repeat(40) {
            val rotation = walkingLookRotation(
                LookDirection(yaw, pitch),
                3.0,
                0.0,
                0.0,
                yawVelocity,
                pitchVelocity,
            )
            yaw = rotation.first
            pitch = rotation.second
        }

        assertTrue(kotlin.math.abs(yaw + 90f) < 2f)
        assertTrue(kotlin.math.abs(pitch) < 1f)
    }
}

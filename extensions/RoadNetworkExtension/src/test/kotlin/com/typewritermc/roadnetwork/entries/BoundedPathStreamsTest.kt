package com.typewritermc.roadnetwork.entries

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoundedPathStreamsTest {
    @Test
    fun `adding beyond the limit disposes the oldest stream`() {
        val first = RecordingStream(1)
        val second = RecordingStream(2)
        val third = RecordingStream(3)
        val fourth = RecordingStream(4)
        val streams = mutableListOf<RecordingStream>()

        streams.addBounded(first, 3, RecordingStream::dispose)
        streams.addBounded(second, 3, RecordingStream::dispose)
        streams.addBounded(third, 3, RecordingStream::dispose)
        streams.addBounded(fourth, 3, RecordingStream::dispose)

        assertTrue(first.disposed)
        assertFalse(second.disposed)
        assertEquals(listOf(2, 3, 4), streams.map { it.id })
    }

    @Test
    fun `a single stream limit replaces the previous stream`() {
        val first = RecordingStream(1)
        val second = RecordingStream(2)
        val streams = mutableListOf<RecordingStream>()

        streams.addBounded(first, 1, RecordingStream::dispose)
        streams.addBounded(second, 1, RecordingStream::dispose)

        assertTrue(first.disposed)
        assertEquals(listOf(2), streams.map { it.id })
    }

    private class RecordingStream(val id: Int) {
        var disposed = false

        fun dispose() {
            disposed = true
        }
    }
}

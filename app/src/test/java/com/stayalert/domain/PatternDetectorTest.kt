package com.stayalert.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternDetectorTest {

    private class FakeClock : Clock {
        var time: Long = 0
        override fun now(): Long = time
    }

    private val width = 1080f
    private val height = 2400f

    private fun tapInRegion(detector: PatternDetector, clock: FakeClock, deltaMs: Long = 100): Boolean {
        clock.time += deltaMs
        return detector.onTouch(x = width - 50f, y = 50f, width = width, height = height)
    }

    @Test
    fun `cuatro toques en la region detectan el patron`() {
        val clock = FakeClock()
        val detector = PatternDetector(clock)

        repeat(3) { tapInRegion(detector, clock) }
        val detected = tapInRegion(detector, clock)

        assertTrue(detected)
    }

    @Test
    fun `toque fuera de la region reinicia el contador`() {
        val clock = FakeClock()
        val detector = PatternDetector(clock)

        repeat(3) { tapInRegion(detector, clock) }
        clock.time += 100
        detector.onTouch(x = 100f, y = 2000f, width = width, height = height)
        val detected = tapInRegion(detector, clock)

        assertFalse(detected)
    }

    @Test
    fun `toque fuera de la ventana temporal reinicia el contador`() {
        val clock = FakeClock()
        val detector = PatternDetector(clock)

        repeat(3) { tapInRegion(detector, clock) }
        clock.time += 1000
        val detected = tapInRegion(detector, clock)

        assertFalse(detected)
    }

    @Test
    fun `tres toques no detectan el patron`() {
        val clock = FakeClock()
        val detector = PatternDetector(clock)

        val first = tapInRegion(detector, clock)
        val second = tapInRegion(detector, clock)
        val third = tapInRegion(detector, clock)

        assertFalse(first)
        assertFalse(second)
        assertFalse(third)
    }

    @Test
    fun `reset limpia el contador`() {
        val clock = FakeClock()
        val detector = PatternDetector(clock)

        repeat(3) { tapInRegion(detector, clock) }
        detector.reset()
        val detected = tapInRegion(detector, clock)

        assertFalse(detected)
    }
}

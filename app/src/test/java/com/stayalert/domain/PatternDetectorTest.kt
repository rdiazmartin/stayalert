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

    private fun tap(detector: PatternDetector, clock: FakeClock, x: Float = 540f, y: Float = 1200f, deltaMs: Long = 100): Boolean {
        clock.time += deltaMs
        return detector.onTouch(x = x, y = y, width = width, height = height)
    }

    @Test
    fun `un toque en cualquier parte detecta el patron`() {
        val clock = FakeClock()
        val detector = PatternDetector(clock)

        val detected = tap(detector, clock)

        assertTrue(detected)
    }

    @Test
    fun `un toque en la esquina superior derecha detecta el patron`() {
        val clock = FakeClock()
        val detector = PatternDetector(clock)

        val detected = tap(detector, clock, x = width - 50f, y = 50f)

        assertTrue(detected)
    }

    @Test
    fun `un toque en la esquina inferior izquierda detecta el patron`() {
        val clock = FakeClock()
        val detector = PatternDetector(clock)

        val detected = tap(detector, clock, x = 50f, y = height - 50f)

        assertTrue(detected)
    }

    @Test
    fun `reset limpia el contador`() {
        val clock = FakeClock()
        val detector = PatternDetector(clock)

        detector.reset()
        val detected = tap(detector, clock)

        assertTrue(detected)
    }
}

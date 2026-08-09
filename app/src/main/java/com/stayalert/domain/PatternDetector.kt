package com.stayalert.domain

class PatternDetector(
    private val clock: Clock,
    private val requiredTaps: Int = 1,
    private val regionFraction: Float = 1.0f,
    private val windowMs: Long = SessionConstants.PATTERN_WINDOW_MS
) {

    private var tapCount = 0
    private var lastTapTime: Long = 0

    fun onTouch(x: Float, y: Float, width: Float, height: Float): Boolean {
        if (width <= 0f || height <= 0f) return false

        val now = clock.now()

        if (!isInRegion(x, y, width, height)) {
            reset()
            return false
        }

        if (tapCount > 0 && now - lastTapTime > windowMs) {
            reset()
        }

        tapCount++
        lastTapTime = now

        if (tapCount >= requiredTaps) {
            reset()
            return true
        }
        return false
    }

    fun reset() {
        tapCount = 0
        lastTapTime = 0
    }

    private fun isInRegion(x: Float, y: Float, width: Float, height: Float): Boolean {
        val regionWidth = width * regionFraction
        val regionHeight = height * regionFraction
        return x >= width - regionWidth && y <= regionHeight
    }
}

package com.stayalert.domain

class SystemClock : Clock {
    override fun now(): Long = android.os.SystemClock.elapsedRealtime()
}

package com.stayalert.domain

enum class ForegroundStatus {
    FOREGROUND,
    NOT_FOREGROUND,
    UNKNOWN
}

interface ForegroundMonitor {
    suspend fun status(packageName: String): ForegroundStatus
}

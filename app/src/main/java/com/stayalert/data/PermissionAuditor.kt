package com.stayalert.data

enum class PermissionState { GRANTED, PENDING }

data class PermissionStatus(
    val permission: Permission,
    val state: PermissionState
)

enum class Permission {
    OVERLAY,
    NOTIFICATIONS
}

interface PermissionAuditor {
    fun canDrawOverlays(): Boolean
    fun areNotificationsEnabled(): Boolean
    fun audit(): List<PermissionStatus>
}

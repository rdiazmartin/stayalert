package com.stayalert.data

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings

class SystemPermissionAuditor(private val context: Context) : PermissionAuditor {

    override fun canDrawOverlays(): Boolean =
        Settings.canDrawOverlays(context)

    override fun areNotificationsEnabled(): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.areNotificationsEnabled()
    }

    override fun audit(): List<PermissionStatus> = listOf(
        PermissionStatus(Permission.OVERLAY, if (canDrawOverlays()) PermissionState.GRANTED else PermissionState.PENDING),
        PermissionStatus(Permission.NOTIFICATIONS, if (areNotificationsEnabled()) PermissionState.GRANTED else PermissionState.PENDING)
    )
}

package com.stayalert.system

import android.app.usage.UsageStatsManager
import android.content.Context
import com.stayalert.domain.ForegroundMonitor
import com.stayalert.domain.ForegroundStatus

class UsageStatsForegroundMonitor(private val context: Context) : ForegroundMonitor {

    override suspend fun status(packageName: String): ForegroundStatus {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ForegroundStatus.UNKNOWN

        val now = System.currentTimeMillis()
        val events = try {
            usageStatsManager.queryEvents(now - 60_000, now)
        } catch (e: SecurityException) {
            android.util.Log.w("UsageStatsForegroundMonitor", "Sin permiso de uso de apps", e)
            return ForegroundStatus.UNKNOWN
        } ?: return ForegroundStatus.UNKNOWN

        var lastEventType = -1
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName) {
                lastEventType = event.eventType
            }
        }

        return when (lastEventType) {
            android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> ForegroundStatus.FOREGROUND
            android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
            android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED -> ForegroundStatus.NOT_FOREGROUND
            else -> ForegroundStatus.UNKNOWN
        }
    }
}

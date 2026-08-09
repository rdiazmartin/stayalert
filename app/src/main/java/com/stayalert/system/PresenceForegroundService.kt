package com.stayalert.system

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.stayalert.data.SystemNotifier

class PresenceForegroundService : Service() {

    private lateinit var notifier: SystemNotifier

    override fun onCreate() {
        super.onCreate()
        notifier = SystemNotifier(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            SystemNotifier.NOTIFICATION_ID_SESSION,
            notifier.buildSessionNotification()
        )
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        notifier.cancelSessionNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.stayalert.system

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.stayalert.StayAlertApplication
import com.stayalert.data.SystemNotifier
import com.stayalert.domain.SessionController
import com.stayalert.domain.SessionEvent
import com.stayalert.domain.SessionState

class PresenceForegroundService : Service() {

    private lateinit var notifier: SystemNotifier

    internal var sessionControllerProvider: (Context) -> SessionController? = { context ->
        (context.applicationContext as? StayAlertApplication)?.container?.sessionController
    }

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
        val controller = sessionControllerProvider(this)
        if (controller == null) {
            android.util.Log.w("PresenceForegroundService", "No se pudo resolver SessionController en onDestroy")
        } else {
            val state = controller.state.value
            if (state is SessionState.Aislada || state is SessionState.Lanzando) {
                controller.emit(SessionEvent.ServiceKilled)
            }
        }
        notifier.cancelSessionNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

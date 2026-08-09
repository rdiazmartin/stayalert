package com.stayalert.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stayalert.StayAlertApplication
import com.stayalert.domain.SessionController
import com.stayalert.domain.SessionEvent

class StopReceiver : BroadcastReceiver() {

    internal var sessionControllerProvider: (Context) -> SessionController? = { context ->
        (context.applicationContext as? StayAlertApplication)?.container?.sessionController
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STOP_SESSION) return
        val controller = sessionControllerProvider(context)
        if (controller == null) {
            android.util.Log.w("StopReceiver", "No se pudo resolver SessionController: el kill switch no actúa")
            return
        }
        controller.emit(SessionEvent.StopRequested)
    }

    companion object {
        const val ACTION_STOP_SESSION = "com.stayalert.action.STOP_SESSION"
    }
}

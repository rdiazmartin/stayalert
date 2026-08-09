package com.stayalert.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stayalert.ui.MainActivity

class StopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_SESSION) {
            val forward = Intent(context, MainActivity::class.java)
                .setAction(ACTION_STOP_SESSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(forward)
        }
    }

    companion object {
        const val ACTION_STOP_SESSION = "com.stayalert.action.STOP_SESSION"
    }
}

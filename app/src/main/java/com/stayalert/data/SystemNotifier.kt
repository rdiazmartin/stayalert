package com.stayalert.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.stayalert.R
import com.stayalert.domain.TerminationReason
import com.stayalert.system.StopReceiver

class SystemNotifier(private val context: Context) : Notifier {

    companion object {
        const val CHANNEL_SESSION = "stayalert_session"
        const val CHANNEL_EVENTS = "stayalert_events"
        const val NOTIFICATION_ID_SESSION = 1
        const val NOTIFICATION_ID_EVENT = 2
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun createChannels() {
        val sessionChannel = NotificationChannel(
            CHANNEL_SESSION,
            "Sesión activa",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificación de sesión activa con acción de detención"
        }
        val eventsChannel = NotificationChannel(
            CHANNEL_EVENTS,
            "Fin de sesión",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos y notificaciones de fin de sesión"
        }
        notificationManager.createNotificationChannel(sessionChannel)
        notificationManager.createNotificationChannel(eventsChannel)
    }

    override fun showSessionNotification() {
        notificationManager.notify(NOTIFICATION_ID_SESSION, buildSessionNotification())
    }

    fun buildSessionNotification(): Notification {
        val stopIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, StopReceiver::class.java).setAction(StopReceiver.ACTION_STOP_SESSION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sesión activa")
            .setContentText("Sesión activa — toca para detener. Capturas bloqueadas.")
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Detener",
                    stopIntent
                ).build()
            )
            .build()
    }

    override fun showSessionEnded(reason: TerminationReason) {
        val notification = Notification.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sesión terminada")
            .setContentText("Sesión terminada: ${reason.text()}")
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_EVENT, notification)
    }

    fun cancelSessionNotification() {
        notificationManager.cancel(NOTIFICATION_ID_SESSION)
    }

    private fun TerminationReason.text(): String = when (this) {
        TerminationReason.Pattern -> "patrón de salida"
        TerminationReason.ManualStop -> "detención manual"
        TerminationReason.ServiceKilled -> "servicio eliminado por el sistema"
        TerminationReason.ScreenOff -> "pantalla apagada"
        TerminationReason.OverlayMissing -> "overlay ausente"
        TerminationReason.PermissionRevoked -> "permiso revocado"
        TerminationReason.TargetLeftForeground -> "la app objetivo salió de primer plano"
        TerminationReason.TargetCrashed -> "la app objetivo se cerró"
        TerminationReason.HideOverlayWindows -> "overlay no dibujado"
        TerminationReason.BatteryCritical -> "batería baja"
        TerminationReason.LaunchFailed -> "no se pudo abrir la app objetivo"
        TerminationReason.OverlayFailed -> "no se pudo desplegar el overlay"
    }
}

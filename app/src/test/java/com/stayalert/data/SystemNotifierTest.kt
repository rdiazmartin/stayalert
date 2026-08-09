package com.stayalert.data

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stayalert.domain.TerminationReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifier = SystemNotifier(context)
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Test
    fun `createChannels crea los dos canales`() {
        notifier.createChannels()
        assertNotNull(notificationManager.getNotificationChannel(SystemNotifier.CHANNEL_SESSION))
        assertNotNull(notificationManager.getNotificationChannel(SystemNotifier.CHANNEL_EVENTS))
    }

    @Test
    fun `showSessionNotification publica la notificacion de sesion`() {
        notifier.createChannels()
        notifier.showSessionNotification()
        val notification = Shadows.shadowOf(notificationManager)
            .getNotification(SystemNotifier.NOTIFICATION_ID_SESSION)
        assertNotNull(notification)
        assertEquals("Sesión activa", notification.extras.getString("android.title"))
    }

    @Test
    fun `showSessionEnded publica la notificacion con el motivo`() {
        notifier.createChannels()
        notifier.showSessionEnded(TerminationReason.Pattern)
        val notification = Shadows.shadowOf(notificationManager)
            .getNotification(SystemNotifier.NOTIFICATION_ID_EVENT)
        assertNotNull(notification)
        assertEquals("Sesión terminada", notification.extras.getString("android.title"))
        assertTrue(notification.extras.getString("android.text")!!.contains("patrón de salida"))
    }

    @Test
    fun `todos los motivos tienen texto`() {
        val reasons = TerminationReason.entries
        assertTrue(reasons.size >= 11)
        for (reason in reasons) {
            notifier.createChannels()
            notifier.showSessionEnded(reason)
            val notification = Shadows.shadowOf(notificationManager)
                .getNotification(SystemNotifier.NOTIFICATION_ID_EVENT)
            assertNotNull("Falta notificación para $reason", notification)
            val text = notification.extras.getString("android.text")
            assertTrue("Falta texto para $reason", text!!.startsWith("Sesión terminada: "))
        }
    }
}

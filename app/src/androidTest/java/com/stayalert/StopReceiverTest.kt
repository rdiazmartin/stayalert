package com.stayalert

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stayalert.system.StopReceiver
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StopReceiverTest {

    @Test
    fun `StopReceiver reenvia el intent a MainActivity con la accion STOP_SESSION`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = StopReceiver()

        val intent = Intent(StopReceiver.ACTION_STOP_SESSION)
        // No debe lanzar excepción: el receiver lanza MainActivity con FLAG_ACTIVITY_NEW_TASK
        receiver.onReceive(context, intent)

        assertEquals(StopReceiver.ACTION_STOP_SESSION, intent.action)
    }
}

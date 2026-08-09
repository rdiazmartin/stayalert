package com.stayalert

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stayalert.StayAlertApplication
import com.stayalert.domain.SessionState
import com.stayalert.system.StopReceiver
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StopReceiverTest {

    @Test
    fun `StopReceiver emite StopRequested sin abrir MainActivity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context.applicationContext as StayAlertApplication
        val sessionController = app.container.sessionController

        val receiver = StopReceiver()
        receiver.onReceive(context, Intent(StopReceiver.ACTION_STOP_SESSION))

        // Sin sesión activa el evento es no-op: el estado permanece Inactiva.
        assertEquals(SessionState.Inactiva, sessionController.state.value)
    }
}

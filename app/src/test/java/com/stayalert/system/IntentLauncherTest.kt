package com.stayalert.system

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stayalert.domain.LaunchError
import com.stayalert.domain.TargetApp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntentLauncherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `launch devuelve exito para una actividad existente`() = runTest {
        val launcher = IntentLauncher(context)
        val result = launcher.launch(
            TargetApp(
                packageName = context.packageName,
                activityName = "com.stayalert.ui.MainActivity"
            )
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `launch devuelve ActivityNotFound para una actividad inexistente`() = runTest {
        val launcher = IntentLauncher(context)
        val result = launcher.launch(
            TargetApp(
                packageName = context.packageName,
                activityName = "com.stayalert.NoExiste"
            )
        )
        assertTrue(result.isFailure)
        assertEquals(LaunchError.ActivityNotFound, result.exceptionOrNull())
    }

    @Test
    fun `launch devuelve ActivityNotFound para un paquete inexistente`() = runTest {
        val launcher = IntentLauncher(context)
        val result = launcher.launch(
            TargetApp(
                packageName = "com.paquete.inexistente.xyz",
                activityName = "com.paquete.inexistente.xyz.MainActivity"
            )
        )
        assertTrue(result.isFailure)
    }
}

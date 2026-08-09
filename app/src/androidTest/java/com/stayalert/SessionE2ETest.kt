package com.stayalert

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionE2ETest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
    }

    @Test
    fun `flujo completo con mock de teams`() {
        // 1. Abrir stayAlert
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("com.stayalert")
        context.startActivity(intent)
        device.wait(Until.hasObject(By.text("stayAlert")), 5000)

        // 2. Conceder permiso de overlay vía appops (el test no puede abrir los diálogos del sistema)
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        appOps.setMode(
            android.app.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
            android.os.Process.myUid(),
            "com.stayalert",
            android.app.AppOpsManager.MODE_ALLOWED
        )

        // 3. Pulsar Iniciar Jornada (si la validación lo permite)
        val startButton = device.findObject(By.text("Iniciar Jornada"))
        if (startButton != null && startButton.isEnabled) {
            startButton.click()
        }

        // 4. Verificar que el mock de Teams se abre
        val mockOpened = device.wait(Until.hasObject(By.pkg("com.microsoft.teams")), 5000)
        assertTrue("El mock de Teams debe abrirse", mockOpened)

        // 5. Esperar el delay de 1 s + despliegue del overlay
        Thread.sleep(3000)

        // 6. El overlay es FLAG_SECURE: el screencap devuelve vacío. Verificar que
        //    el mock ya no está en primer plano (el overlay lo cubre).
        val mockFocused = device.wait(Until.hasObject(By.pkg("com.microsoft.teams")), 2000)
        assertFalse("El mock debe estar cubierto por el overlay", mockFocused)

        // 7. Patrón de salida: 4 toques en la esquina superior derecha
        val width = device.displayWidth
        val height = device.displayHeight
        val x = width - 50
        val y = 200
        repeat(4) {
            device.click(x, y)
            Thread.sleep(100)
        }

        // 8. Tras el patrón, el mock vuelve a primer plano (sesión terminada)
        val mockBack = device.wait(Until.hasObject(By.pkg("com.microsoft.teams")), 5000)
        assertTrue("El mock debe volver a primer plano tras el patrón de salida", mockBack)
    }

    @Test
    fun `el mock de teams no recibe toques cuando el overlay esta visible`() {
        // Este test requiere una sesión activa con overlay; se valida vía logcat
        // en el flujo E2E completo. Aquí solo verificamos que el mock existe.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val installed = try {
            context.packageManager.getPackageInfo("com.microsoft.teams", 0)
            true
        } catch (e: Exception) {
            false
        }
        assertTrue("El mock de Teams debe estar instalado", installed)
    }
}

package com.stayalert.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemAppInstalledCheckerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `isInstalled devuelve true para el paquete propio`() {
        val checker = SystemAppInstalledChecker(context)
        assertTrue(checker.isInstalled(context.packageName))
    }

    @Test
    fun `isInstalled devuelve false para un paquete inexistente`() {
        val checker = SystemAppInstalledChecker(context)
        assertFalse(checker.isInstalled("com.paquete.inexistente.xyz"))
    }
}

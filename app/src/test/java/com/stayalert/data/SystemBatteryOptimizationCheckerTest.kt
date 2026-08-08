package com.stayalert.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemBatteryOptimizationCheckerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `isIgnoringBatteryOptimizations refleja el estado del sistema`() {
        val checker = SystemBatteryOptimizationChecker(context)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        assertEquals(
            powerManager.isIgnoringBatteryOptimizations(context.packageName),
            checker.isIgnoringBatteryOptimizations()
        )
    }
}

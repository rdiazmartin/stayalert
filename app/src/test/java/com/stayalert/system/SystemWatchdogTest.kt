package com.stayalert.system

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.stayalert.domain.ForegroundMonitor
import com.stayalert.domain.ForegroundStatus
import com.stayalert.domain.OverlayController
import com.stayalert.domain.SessionConstants
import com.stayalert.domain.SessionEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemWatchdogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private class FakeMonitor(private val status: ForegroundStatus) : ForegroundMonitor {
        override suspend fun status(packageName: String): ForegroundStatus = status
    }

    private class FakeOverlay(private val visible: Boolean) : OverlayController {
        override fun show() {}
        override fun hide() {}
        override fun isVisible(): Boolean = visible
    }

    private fun createWatchdog(
        scope: kotlinx.coroutines.CoroutineScope,
        events: MutableList<SessionEvent>,
        monitorStatus: ForegroundStatus = ForegroundStatus.FOREGROUND,
        overlayVisible: Boolean = true
    ): SystemWatchdog = SystemWatchdog(
        context = context,
        scope = scope,
        foregroundMonitor = FakeMonitor(monitorStatus),
        overlayController = FakeOverlay(overlayVisible),
        targetPackage = { "com.microsoft.teams" },
        onEvent = { events.add(it) }
    )

    @Test
    fun `pantalla apagada emite ScreenOff`() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<SessionEvent>()
        val watchdog = createWatchdog(backgroundScope, events)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        Shadows.shadowOf(powerManager).setIsInteractive(false)

        watchdog.start()
        advanceTimeBy(SessionConstants.WATCHDOG_POLL_MS + 100)
        watchdog.stop()

        assertTrue(events.contains(SessionEvent.ScreenOff))
    }

    @Test
    fun `permiso revocado emite PermissionRevoked`() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<SessionEvent>()
        val watchdog = createWatchdog(backgroundScope, events)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        Shadows.shadowOf(powerManager).setIsInteractive(true)

        watchdog.start()
        advanceTimeBy(SessionConstants.WATCHDOG_POLL_MS + 100)
        watchdog.stop()

        assertTrue(events.contains(SessionEvent.PermissionRevoked))
    }

    @Test
    fun `overlay ausente emite OverlayMissing`() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<SessionEvent>()
        val watchdog = createWatchdog(backgroundScope, events, overlayVisible = false)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        Shadows.shadowOf(powerManager).setIsInteractive(true)
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true)

        watchdog.start()
        advanceTimeBy(SessionConstants.WATCHDOG_POLL_MS + 100)
        watchdog.stop()

        assertTrue(events.contains(SessionEvent.OverlayMissing))
    }

    @Test
    fun `app objetivo fuera de primer plano emite TargetLeftForeground`() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<SessionEvent>()
        val watchdog = createWatchdog(backgroundScope, events, monitorStatus = ForegroundStatus.NOT_FOREGROUND)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        Shadows.shadowOf(powerManager).setIsInteractive(true)
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true)

        watchdog.start()
        advanceTimeBy(SessionConstants.WATCHDOG_POLL_MS + 100)
        watchdog.stop()

        assertTrue(events.contains(SessionEvent.TargetLeftForeground))
    }

    @Test
    fun `bateria baja emite BatteryWarning`() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<SessionEvent>()
        val watchdog = createWatchdog(backgroundScope, events)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        Shadows.shadowOf(powerManager).setIsInteractive(true)
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true)
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        Shadows.shadowOf(batteryManager).setIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY, SessionConstants.BATTERY_WARNING_LEVEL)

        watchdog.start()
        advanceTimeBy(SessionConstants.WATCHDOG_POLL_MS + 100)
        watchdog.stop()

        assertTrue(events.any { it is SessionEvent.BatteryWarning })
    }

    @Test
    fun `bateria critica emite BatteryCritical`() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<SessionEvent>()
        val watchdog = createWatchdog(backgroundScope, events)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        Shadows.shadowOf(powerManager).setIsInteractive(true)
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true)
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        Shadows.shadowOf(batteryManager).setIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY, SessionConstants.BATTERY_CRITICAL_LEVEL)

        watchdog.start()
        advanceTimeBy(SessionConstants.WATCHDOG_POLL_MS + 100)
        watchdog.stop()

        assertTrue(events.any { it is SessionEvent.BatteryCritical })
    }
}

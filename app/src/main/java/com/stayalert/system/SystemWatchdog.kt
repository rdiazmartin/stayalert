package com.stayalert.system

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import com.stayalert.domain.ForegroundMonitor
import com.stayalert.domain.ForegroundStatus
import com.stayalert.domain.OverlayController
import com.stayalert.domain.SessionConstants
import com.stayalert.domain.SessionEvent
import com.stayalert.domain.Watchdog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SystemWatchdog(
    private val context: Context,
    private val scope: CoroutineScope,
    private val foregroundMonitor: ForegroundMonitor,
    private val overlayController: OverlayController,
    private val targetPackage: suspend () -> String,
    private val onEvent: (SessionEvent) -> Unit
) : Watchdog {

    private var job: Job? = null

    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                check()
                delay(SessionConstants.WATCHDOG_POLL_MS)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun check() {
        // Limitación documentada (AD-5): HIDE_OVERLAY_WINDOWS no es detectable directamente.
        // El sistema no expone si el overlay se dibuja sobre la app objetivo; la detección
        // real requeriría WindowManager internals. Se cubre indirectamente vía
        // TargetLeftForeground (app fuera de primer plano) y OverlayMissing (overlay ausente).
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) {
            onEvent(SessionEvent.ScreenOff)
            return
        }

        if (!Settings.canDrawOverlays(context)) {
            onEvent(SessionEvent.PermissionRevoked)
            return
        }

        if (!overlayController.isVisible()) {
            onEvent(SessionEvent.OverlayMissing)
            return
        }

        val status = foregroundMonitor.status(targetPackage())
        if (status == ForegroundStatus.NOT_FOREGROUND) {
            onEvent(SessionEvent.TargetLeftForeground)
            return
        }

        // Nota: si la anomalía persiste, el evento se re-emite en cada poll (2 s).
        // SessionController es idempotente (AD-9): el primer evento termina la sesión
        // y los siguientes son no-ops en Inactiva. Ruido aceptable, sin daño funcional.
        val batteryLevel = readBatteryLevel()
        if (batteryLevel != null) {
            if (batteryLevel <= SessionConstants.BATTERY_CRITICAL_LEVEL) {
                onEvent(SessionEvent.BatteryCritical(batteryLevel))
            } else if (batteryLevel <= SessionConstants.BATTERY_WARNING_LEVEL) {
                onEvent(SessionEvent.BatteryWarning(batteryLevel))
            }
        }
    }

    private fun readBatteryLevel(): Int? {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return null
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}

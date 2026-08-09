package com.stayalert.system

import android.content.Context
import android.content.Intent
import com.stayalert.data.SettingsRepository
import com.stayalert.domain.ForegroundMonitor
import com.stayalert.domain.ForegroundStatus
import com.stayalert.domain.LaunchError
import com.stayalert.domain.OverlayController
import com.stayalert.domain.SessionCommand
import com.stayalert.domain.SessionConstants
import com.stayalert.domain.SessionEvent
import com.stayalert.domain.TargetApp
import com.stayalert.domain.TargetAppLauncher
import com.stayalert.domain.Watchdog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SessionCommandHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
    private val targetAppLauncher: TargetAppLauncher,
    private val foregroundMonitor: ForegroundMonitor,
    private val overlayController: OverlayController,
    private val watchdog: Watchdog,
    private val onEvent: (SessionEvent) -> Unit
) {

    fun handle(command: SessionCommand) {
        when (command) {
            SessionCommand.LaunchTarget -> launchTarget()
            SessionCommand.ShowOverlay -> overlayController.show()
            SessionCommand.HideOverlay -> hideOverlay()
            SessionCommand.StartFgs -> startFgs()
            SessionCommand.StopFgs -> stopFgs()
            SessionCommand.StartWatchdog -> watchdog.start()
            SessionCommand.StopWatchdog -> watchdog.stop()
        }
    }

    private fun hideOverlay() {
        scope.launch {
            try {
                overlayController.hide()
                onEvent(SessionEvent.OverlayHidden)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                runCatching {
                    android.util.Log.e("SessionCommandHandler", "No se pudo ocultar el overlay", e)
                }
                onEvent(SessionEvent.OverlayHideFailed(e.message ?: "desconocido"))
            }
        }
    }

    private fun startFgs() {
        val intent = Intent(context, PresenceForegroundService::class.java)
        context.startForegroundService(intent)
    }

    private fun stopFgs() {
        context.stopService(Intent(context, PresenceForegroundService::class.java))
    }

    private fun launchTarget() {
        scope.launch {
            val target = TargetApp(
                packageName = settingsRepository.targetPackage.first(),
                activityName = settingsRepository.targetActivity.first()
            )
            android.util.Log.d("SessionCommandHandler", "Lanzando objetivo: ${target.packageName}/${target.activityName}")
            val result = targetAppLauncher.launch(target)
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                val reason = when (error) {
                    is LaunchError.PackageNotInstalled -> "paquete no instalado"
                    is LaunchError.ActivityNotFound -> "actividad no resuelta"
                    is LaunchError.SystemFailure -> error.cause.message ?: "fallo del sistema"
                    else -> "fallo desconocido"
                }
                android.util.Log.e("SessionCommandHandler", "LaunchFailed: $reason", error)
                onEvent(
                    SessionEvent.LaunchFailed(reason)
                )
                return@launch
            }

            delay(SessionConstants.LAUNCH_DELAY_MS)

            val status = foregroundMonitor.status(target.packageName)
            android.util.Log.d("SessionCommandHandler", "Foreground de ${target.packageName} tras ${SessionConstants.LAUNCH_DELAY_MS} ms: $status")
            if (status == ForegroundStatus.NOT_FOREGROUND) {
                onEvent(SessionEvent.LaunchFailed("la app objetivo no está en primer plano"))
                return@launch
            }

            overlayController.show()
        }
    }
}

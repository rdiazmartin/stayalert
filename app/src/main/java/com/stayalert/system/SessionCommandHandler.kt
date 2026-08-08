package com.stayalert.system

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SessionCommandHandler(
    private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
    private val targetAppLauncher: TargetAppLauncher,
    private val foregroundMonitor: ForegroundMonitor,
    private val overlayController: OverlayController,
    private val onEvent: (SessionEvent) -> Unit
) {

    fun handle(command: SessionCommand) {
        when (command) {
            SessionCommand.LaunchTarget -> launchTarget()
            SessionCommand.ShowOverlay -> overlayController.show()
            SessionCommand.HideOverlay -> overlayController.hide()
            SessionCommand.StartFgs,
            SessionCommand.StopFgs,
            SessionCommand.StartWatchdog,
            SessionCommand.StopWatchdog -> {
                // no-op: FGS y watchdog en stories 2.4/2.5
            }
        }
    }

    private fun launchTarget() {
        scope.launch {
            val target = TargetApp(
                packageName = settingsRepository.targetPackage.first(),
                activityName = settingsRepository.targetActivity.first()
            )
            val result = targetAppLauncher.launch(target)
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                onEvent(
                    SessionEvent.LaunchFailed(
                        when (error) {
                            is LaunchError.PackageNotInstalled -> "paquete no instalado"
                            is LaunchError.ActivityNotFound -> "actividad no resuelta"
                            is LaunchError.SystemFailure -> error.cause.message ?: "fallo del sistema"
                            else -> "fallo desconocido"
                        }
                    )
                )
                return@launch
            }

            delay(SessionConstants.LAUNCH_DELAY_MS)

            val status = foregroundMonitor.status(target.packageName)
            if (status == ForegroundStatus.NOT_FOREGROUND) {
                onEvent(SessionEvent.LaunchFailed("la app objetivo no está en primer plano"))
                return@launch
            }

            overlayController.show()
        }
    }
}

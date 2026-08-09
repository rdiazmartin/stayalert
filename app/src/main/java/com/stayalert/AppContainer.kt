package com.stayalert

import android.app.Application
import com.stayalert.data.DataStoreSettingsRepository
import com.stayalert.data.SystemAppInstalledChecker
import com.stayalert.data.SystemBatteryOptimizationChecker
import com.stayalert.data.SystemNotifier
import com.stayalert.data.SystemPermissionAuditor
import com.stayalert.domain.PatternDetector
import com.stayalert.domain.SessionController
import com.stayalert.domain.SessionEvent
import com.stayalert.domain.SessionValidator
import com.stayalert.domain.SystemClock
import com.stayalert.system.IntentLauncher
import com.stayalert.system.SessionCommandHandler
import com.stayalert.system.SystemOverlayController
import com.stayalert.system.SystemWatchdog
import com.stayalert.system.UsageStatsForegroundMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

class StayAlertApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onTerminate() {
        container.appScope.cancel()
        super.onTerminate()
    }
}

class AppContainer(private val app: Application) {

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settingsRepository by lazy { DataStoreSettingsRepository(app) }
    val permissionAuditor by lazy { SystemPermissionAuditor(app) }
    val appInstalledChecker by lazy { SystemAppInstalledChecker(app) }
    val batteryOptimizationChecker by lazy { SystemBatteryOptimizationChecker(app) }
    val notifier by lazy { SystemNotifier(app) }

    private val clock = SystemClock()
    private val foregroundMonitor by lazy { UsageStatsForegroundMonitor(app) }

    val overlayController: SystemOverlayController by lazy {
        SystemOverlayController(
            context = app,
            scope = appScope,
            clock = clock,
            patternDetector = PatternDetector(clock = clock),
            onEvent = { event -> sessionController.emit(event) }
        )
    }

    val watchdog: SystemWatchdog by lazy {
        SystemWatchdog(
            context = app,
            scope = appScope,
            foregroundMonitor = foregroundMonitor,
            overlayController = overlayController,
            targetPackage = { settingsRepository.targetPackage.first() },
            onEvent = { event -> sessionController.emit(event) }
        )
    }

    val sessionController: SessionController by lazy {
        SessionController(
            scope = appScope,
            validator = SessionValidator(
                permissionAuditor,
                settingsRepository,
                appInstalledChecker
            ),
            onCommand = { command -> commandHandler.handle(command) }
        )
    }

    val commandHandler: SessionCommandHandler by lazy {
        SessionCommandHandler(
            context = app,
            scope = appScope,
            settingsRepository = settingsRepository,
            targetAppLauncher = IntentLauncher(app),
            foregroundMonitor = foregroundMonitor,
            overlayController = overlayController,
            notifier = notifier,
            watchdog = watchdog,
            onEvent = { event -> sessionController.emit(event) }
        )
    }
}

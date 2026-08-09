package com.stayalert.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stayalert.data.DataStoreSettingsRepository
import com.stayalert.data.SystemAppInstalledChecker
import com.stayalert.data.SystemBatteryOptimizationChecker
import com.stayalert.data.SystemNotifier
import com.stayalert.data.SystemPermissionAuditor
import com.stayalert.domain.PatternDetector
import com.stayalert.domain.SessionController
import com.stayalert.domain.SessionEvent
import com.stayalert.domain.SessionState
import com.stayalert.domain.SessionValidator
import com.stayalert.domain.SystemClock
import com.stayalert.domain.TerminationReason
import com.stayalert.domain.ValidationFailure
import com.stayalert.system.IntentLauncher
import com.stayalert.system.SessionCommandHandler
import com.stayalert.system.StopReceiver
import com.stayalert.system.SystemOverlayController
import com.stayalert.system.SystemWatchdog
import com.stayalert.system.UsageStatsForegroundMonitor
import com.stayalert.ui.components.ResponsibleUseNotice
import com.stayalert.ui.settings.SettingsScreen
import com.stayalert.ui.settings.SettingsViewModel
import com.stayalert.ui.theme.Accent
import com.stayalert.ui.theme.AccentOn
import com.stayalert.ui.theme.InkDisabled
import com.stayalert.ui.theme.SurfaceBase
import com.stayalert.ui.theme.StayAlertTheme
import com.stayalert.ui.viewmodel.MainViewModel
import com.stayalert.R

class MainActivity : ComponentActivity() {

    private val settingsRepository by lazy {
        DataStoreSettingsRepository(applicationContext)
    }

    private val permissionAuditor by lazy {
        SystemPermissionAuditor(applicationContext)
    }

    private val appInstalledChecker by lazy {
        SystemAppInstalledChecker(applicationContext)
    }

    private val batteryOptimizationChecker by lazy {
        SystemBatteryOptimizationChecker(applicationContext)
    }

    private lateinit var sessionCommandHandler: SessionCommandHandler

    private val sessionController by lazy {
        SessionController(
            scope = lifecycleScope,
            validator = SessionValidator(
                permissionAuditor,
                settingsRepository,
                appInstalledChecker
            ),
            onCommand = { command ->
                sessionCommandHandler.handle(command)
            }
        )
    }

    private val overlayController by lazy {
        SystemOverlayController(
            context = applicationContext,
            scope = lifecycleScope,
            clock = SystemClock(),
            patternDetector = PatternDetector(clock = SystemClock()),
            onEvent = { event -> sessionController.emit(event) }
        )
    }

    private val commandHandler by lazy {
        SessionCommandHandler(
            context = applicationContext,
            scope = lifecycleScope,
            settingsRepository = settingsRepository,
            targetAppLauncher = IntentLauncher(applicationContext),
            foregroundMonitor = UsageStatsForegroundMonitor(applicationContext),
            overlayController = overlayController,
            notifier = notifier,
            watchdog = watchdog,
            onEvent = { event -> sessionController.emit(event) }
        ).also { sessionCommandHandler = it }
    }

    private val watchdog by lazy {
        SystemWatchdog(
            context = applicationContext,
            scope = lifecycleScope,
            foregroundMonitor = UsageStatsForegroundMonitor(applicationContext),
            overlayController = overlayController,
            targetPackage = { settingsRepository.targetPackage.first() },
            onEvent = { event -> sessionController.emit(event) }
        )
    }

    private val notifier by lazy {
        SystemNotifier(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == StopReceiver.ACTION_STOP_SESSION) {
            sessionController.emit(SessionEvent.StopRequested)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        commandHandler
        notifier.createChannels()
        setContent {
            StayAlertTheme {
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(settingsRepository, sessionController)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        permissionAuditor,
                        settingsRepository,
                        appInstalledChecker,
                        batteryOptimizationChecker
                    )
                )
                var showSettings by remember { mutableStateOf(false) }

                if (showSettings) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { showSettings = false }
                    )
                } else {
                    MainScreen(
                        viewModel = mainViewModel,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit
) {
    val noticeAccepted by viewModel.noticeAccepted.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val startError by viewModel.startError.collectAsStateWithLifecycle()
    val validationFailure by viewModel.validationFailure.collectAsStateWithLifecycle()
    val lastTerminationReason by viewModel.lastTerminationReason.collectAsStateWithLifecycle()

    val isLanzando = sessionState is SessionState.Lanzando
    val canStart = noticeAccepted && validationFailure == null && !isLanzando

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "stayAlert",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    viewModel.clearStartError()
                    viewModel.startSession()
                },
                enabled = canStart,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = AccentOn,
                    disabledContainerColor = InkDisabled,
                    disabledContentColor = SurfaceBase
                )
            ) {
                if (isLanzando) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = AccentOn,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = if (isLanzando) "Abriendo app objetivo…" else "Iniciar Jornada",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            val message = startError?.message() ?: validationFailure?.message()
            if (message != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            lastTerminationReason?.let { reason ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sesión terminada: ${reason.text()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                contentDescription = "Configuración",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    if (!noticeAccepted) {
        ResponsibleUseNotice(onAccept = viewModel::acceptNotice)
    }
}

private fun ValidationFailure.message(): String = when (this) {
    ValidationFailure.PERMISSION_OVERLAY -> "Falta el permiso de overlay. Tócalo para abrir Ajustes."
    ValidationFailure.PERMISSION_NOTIFICATIONS -> "Falta el permiso de notificaciones. Tócalo para abrir Ajustes."
    ValidationFailure.NOTICE_NOT_ACCEPTED -> "Debes aceptar el aviso de uso responsable."
    ValidationFailure.TARGET_NOT_INSTALLED -> "La app objetivo no está instalada."
}

private fun TerminationReason.text(): String = when (this) {
    TerminationReason.Pattern -> "patrón de salida"
    TerminationReason.ManualStop -> "detención manual"
    TerminationReason.ScreenOff -> "pantalla apagada"
    TerminationReason.OverlayMissing -> "overlay ausente"
    TerminationReason.PermissionRevoked -> "permiso revocado"
    TerminationReason.TargetLeftForeground -> "la app objetivo salió de primer plano"
    TerminationReason.TargetCrashed -> "la app objetivo se cerró"
    TerminationReason.HideOverlayWindows -> "overlay no dibujado"
    TerminationReason.BatteryCritical -> "batería baja"
    TerminationReason.LaunchFailed -> "no se pudo abrir la app objetivo"
    TerminationReason.OverlayFailed -> "no se pudo desplegar el overlay"
}

package com.stayalert.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionController(
    private val scope: CoroutineScope,
    private val validator: SessionValidator,
    private val onCommand: (SessionCommand) -> Unit
) {

    private val _state = MutableStateFlow<SessionState>(SessionState.Inactiva)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val events = Channel<SessionEvent>(Channel.UNLIMITED)

    private val _lastTerminationReason = MutableStateFlow<TerminationReason?>(null)
    val lastTerminationReason: StateFlow<TerminationReason?> = _lastTerminationReason.asStateFlow()

    init {
        scope.launch {
            for (event in events) {
                try {
                    handle(event)
                } catch (e: Exception) {
                    android.util.Log.e("SessionController", "Error manejando el evento $event", e)
                }
            }
        }
    }

    suspend fun startSession(): ValidationFailure? {
        val failure = validator.validate()
        if (failure != null) return failure
        _state.value = SessionState.Lanzando
        onCommand(SessionCommand.LaunchTarget)
        return null
    }

    suspend fun validate(): ValidationFailure? = validator.validate()

    fun clearLastTerminationReason() {
        _lastTerminationReason.value = null
    }

    fun emit(event: SessionEvent) {
        val sent = events.trySend(event)
        if (sent.isFailure) {
            android.util.Log.e("SessionController", "No se pudo encolar el evento $event")
        }
    }

    private fun handle(event: SessionEvent) {
        when (val current = _state.value) {
            SessionState.Inactiva -> handleInactiva(event)
            SessionState.Lanzando -> handleLanzando(event)
            SessionState.Aislada -> handleAislada(event)
            SessionState.Deteniendo -> handleDeteniendo(event)
        }
    }

    private fun handleInactiva(event: SessionEvent) {
        when (event) {
            is SessionEvent.StopRequested,
            is SessionEvent.PatternDetected,
            is SessionEvent.OverlayShown,
            is SessionEvent.OverlayFailed,
            is SessionEvent.LaunchFailed,
            is SessionEvent.ScreenOff,
            is SessionEvent.OverlayMissing,
            is SessionEvent.PermissionRevoked,
            is SessionEvent.TargetLeftForeground,
            is SessionEvent.TargetCrashed,
            is SessionEvent.HideOverlayWindows,
            is SessionEvent.BatteryWarning,
            is SessionEvent.BatteryCritical -> {
                // no-op: eventos no admitidos en Inactiva (AD-9)
            }
        }
    }

    private fun handleLanzando(event: SessionEvent) {
        when (event) {
            is SessionEvent.OverlayShown -> {
                _state.value = SessionState.Aislada
                onCommand(SessionCommand.StartFgs)
                onCommand(SessionCommand.StartWatchdog)
            }
            is SessionEvent.LaunchFailed -> terminate(TerminationReason.LaunchFailed)
            is SessionEvent.OverlayFailed -> terminate(TerminationReason.OverlayFailed)
            is SessionEvent.StopRequested -> terminate(TerminationReason.ManualStop)
            is SessionEvent.PatternDetected,
            is SessionEvent.ScreenOff,
            is SessionEvent.OverlayMissing,
            is SessionEvent.PermissionRevoked,
            is SessionEvent.TargetLeftForeground,
            is SessionEvent.TargetCrashed,
            is SessionEvent.HideOverlayWindows,
            is SessionEvent.BatteryWarning,
            is SessionEvent.BatteryCritical -> {
                // no-op: anomalías no admitidas en Lanzando (AD-9)
            }
        }
    }

    private fun handleAislada(event: SessionEvent) {
        when (event) {
            is SessionEvent.PatternDetected -> terminate(TerminationReason.Pattern)
            is SessionEvent.StopRequested -> terminate(TerminationReason.ManualStop)
            is SessionEvent.ScreenOff -> terminate(TerminationReason.ScreenOff)
            is SessionEvent.OverlayMissing -> terminate(TerminationReason.OverlayMissing)
            is SessionEvent.PermissionRevoked -> terminate(TerminationReason.PermissionRevoked)
            is SessionEvent.TargetLeftForeground -> terminate(TerminationReason.TargetLeftForeground)
            is SessionEvent.TargetCrashed -> terminate(TerminationReason.TargetCrashed)
            is SessionEvent.HideOverlayWindows -> terminate(TerminationReason.HideOverlayWindows)
            is SessionEvent.BatteryCritical -> terminate(TerminationReason.BatteryCritical)
            is SessionEvent.BatteryWarning -> {
                // no-op: aviso de batería no termina la sesión (FR-16)
            }
            is SessionEvent.OverlayShown,
            is SessionEvent.OverlayFailed,
            is SessionEvent.LaunchFailed -> {
                // no-op: eventos de despliegue no admitidos en Aislada (AD-9)
            }
        }
    }

    private fun handleDeteniendo(event: SessionEvent) {
        when (event) {
            is SessionEvent.StopRequested,
            is SessionEvent.PatternDetected,
            is SessionEvent.ScreenOff,
            is SessionEvent.OverlayMissing,
            is SessionEvent.PermissionRevoked,
            is SessionEvent.TargetLeftForeground,
            is SessionEvent.TargetCrashed,
            is SessionEvent.HideOverlayWindows,
            is SessionEvent.BatteryCritical,
            is SessionEvent.LaunchFailed,
            is SessionEvent.OverlayFailed -> {
                // no-op: terminación idempotente (AD-9)
            }
            is SessionEvent.OverlayShown,
            is SessionEvent.BatteryWarning -> {
                // no-op
            }
        }
    }

    private fun terminate(reason: TerminationReason) {
        if (_state.value is SessionState.Deteniendo || _state.value is SessionState.Inactiva) return
        _state.value = SessionState.Deteniendo
        _lastTerminationReason.value = reason
        onCommand(SessionCommand.HideOverlay)
        onCommand(SessionCommand.StopFgs)
        onCommand(SessionCommand.StopWatchdog)
        _state.value = SessionState.Inactiva
    }
}

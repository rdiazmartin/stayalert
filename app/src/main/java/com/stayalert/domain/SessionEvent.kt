package com.stayalert.domain

sealed class SessionEvent {
    data object PatternDetected : SessionEvent()
    data object StopRequested : SessionEvent()
    data object ServiceKilled : SessionEvent()
    data object OverlayShown : SessionEvent()
    data class OverlayFailed(val cause: String) : SessionEvent()
    data class LaunchFailed(val cause: String) : SessionEvent()
    data object ScreenOff : SessionEvent()
    data object OverlayMissing : SessionEvent()
    data object PermissionRevoked : SessionEvent()
    data object TargetLeftForeground : SessionEvent()
    data object TargetCrashed : SessionEvent()
    data object HideOverlayWindows : SessionEvent()
    data class BatteryWarning(val level: Int) : SessionEvent()
    data class BatteryCritical(val level: Int) : SessionEvent()
}

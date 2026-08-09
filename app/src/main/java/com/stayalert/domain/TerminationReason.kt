package com.stayalert.domain

enum class TerminationReason {
    Pattern,
    ManualStop,
    ScreenOff,
    OverlayMissing,
    PermissionRevoked,
    TargetLeftForeground,
    TargetCrashed,
    HideOverlayWindows,
    BatteryCritical,
    LaunchFailed,
    OverlayFailed,
    ServiceKilled
}

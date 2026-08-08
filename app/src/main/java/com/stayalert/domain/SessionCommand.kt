package com.stayalert.domain

sealed class SessionCommand {
    data object ShowOverlay : SessionCommand()
    data object HideOverlay : SessionCommand()
    data object StartFgs : SessionCommand()
    data object StopFgs : SessionCommand()
    data object StartWatchdog : SessionCommand()
    data object StopWatchdog : SessionCommand()
    data object LaunchTarget : SessionCommand()
}

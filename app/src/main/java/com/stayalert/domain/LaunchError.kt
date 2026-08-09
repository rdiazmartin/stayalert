package com.stayalert.domain

sealed class LaunchError(message: String) : Exception(message) {
    data object PackageNotInstalled : LaunchError("paquete no instalado")
    data object ActivityNotFound : LaunchError("actividad no resuelta")
    data class SystemFailure(override val cause: Throwable) : LaunchError(cause.message ?: "fallo del sistema")
}

package com.stayalert.domain

sealed class SessionState {
    data object Inactiva : SessionState()
    data object Lanzando : SessionState()
    data object Aislada : SessionState()
    data object Deteniendo : SessionState()
}

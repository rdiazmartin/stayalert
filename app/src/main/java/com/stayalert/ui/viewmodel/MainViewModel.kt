package com.stayalert.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stayalert.data.SettingsRepository
import com.stayalert.domain.SessionController
import com.stayalert.domain.SessionState
import com.stayalert.domain.TerminationReason
import com.stayalert.domain.ValidationFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val sessionController: SessionController
) : ViewModel() {

    val noticeAccepted: StateFlow<Boolean> = settingsRepository.noticeAccepted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val sessionState: StateFlow<SessionState> = sessionController.state

    val lastTerminationReason: StateFlow<TerminationReason?> = sessionController.lastTerminationReason

    private val _startError = MutableStateFlow<ValidationFailure?>(null)
    val startError: StateFlow<ValidationFailure?> = _startError

    private val _validationFailure = MutableStateFlow<ValidationFailure?>(null)
    val validationFailure: StateFlow<ValidationFailure?> = _validationFailure

    init {
        refreshValidation()
    }

    fun refreshValidation() {
        viewModelScope.launch {
            _validationFailure.value = sessionController.validate()
        }
    }

    fun acceptNotice() {
        viewModelScope.launch {
            try {
                settingsRepository.setNoticeAccepted(true)
                refreshValidation()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "No se pudo persistir la aceptación del aviso", e)
            }
        }
    }

    fun startSession() {
        viewModelScope.launch {
            _startError.value = sessionController.startSession()
        }
    }

    fun clearStartError() {
        _startError.value = null
    }

    fun clearTerminationFeedback() {
        sessionController.clearLastTerminationReason()
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val sessionController: SessionController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            return MainViewModel(settingsRepository, sessionController) as T
        }
    }
}

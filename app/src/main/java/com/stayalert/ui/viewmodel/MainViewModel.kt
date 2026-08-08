package com.stayalert.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stayalert.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val noticeAccepted: StateFlow<Boolean> = settingsRepository.noticeAccepted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    fun acceptNotice() {
        viewModelScope.launch {
            try {
                settingsRepository.setNoticeAccepted(true)
            } catch (e: Exception) {
                Log.e("MainViewModel", "No se pudo persistir la aceptación del aviso", e)
            }
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            return MainViewModel(settingsRepository) as T
        }
    }
}

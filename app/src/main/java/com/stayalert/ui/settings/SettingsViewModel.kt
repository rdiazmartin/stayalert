package com.stayalert.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stayalert.data.PermissionAuditor
import com.stayalert.data.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val permissionAuditor: PermissionAuditor
) : ViewModel() {

    private val _permissions = MutableStateFlow<List<PermissionStatus>>(emptyList())
    val permissions: StateFlow<List<PermissionStatus>> = _permissions

    init {
        refresh()
    }

    fun refresh() {
        _permissions.value = permissionAuditor.audit()
    }

    class Factory(
        private val permissionAuditor: PermissionAuditor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(permissionAuditor) as T
        }
    }
}

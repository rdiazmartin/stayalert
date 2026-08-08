package com.stayalert.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stayalert.data.AppInstalledChecker
import com.stayalert.data.BatteryOptimizationChecker
import com.stayalert.data.PermissionAuditor
import com.stayalert.data.PermissionStatus
import com.stayalert.data.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SettingsViewModel(
    private val permissionAuditor: PermissionAuditor,
    private val settingsRepository: SettingsRepository,
    private val appInstalledChecker: AppInstalledChecker,
    private val batteryOptimizationChecker: BatteryOptimizationChecker
) : ViewModel() {

    private val _permissions = MutableStateFlow<List<PermissionStatus>>(emptyList())
    val permissions: StateFlow<List<PermissionStatus>> = _permissions

    private val _targetPackageInput = MutableStateFlow("")
    val targetPackage: StateFlow<String> = _targetPackageInput

    private val _targetActivityInput = MutableStateFlow("")
    val targetActivity: StateFlow<String> = _targetActivityInput

    private val _batteryExempt = MutableStateFlow(false)
    val batteryExempt: StateFlow<Boolean> = _batteryExempt

    private val _targetInstalled = MutableStateFlow(false)
    val targetInstalled: StateFlow<Boolean> = _targetInstalled

    init {
        viewModelScope.launch {
            settingsRepository.targetPackage
                .stateIn(viewModelScope, SharingStarted.Eagerly, "")
                .collect { _targetPackageInput.value = it }
        }
        viewModelScope.launch {
            settingsRepository.targetActivity
                .stateIn(viewModelScope, SharingStarted.Eagerly, "")
                .collect { _targetActivityInput.value = it }
        }
        viewModelScope.launch {
            _targetPackageInput
                .debounce(500)
                .distinctUntilChanged()
                .collect { value ->
                    settingsRepository.setTargetPackage(value)
                    _targetInstalled.value = appInstalledChecker.isInstalled(value)
                }
        }
        viewModelScope.launch {
            _targetActivityInput
                .debounce(500)
                .distinctUntilChanged()
                .collect { value ->
                    settingsRepository.setTargetActivity(value)
                }
        }
        refresh()
    }

    fun refresh() {
        _permissions.value = permissionAuditor.audit()
        _batteryExempt.value = batteryOptimizationChecker.isIgnoringBatteryOptimizations()
        _targetInstalled.value = appInstalledChecker.isInstalled(_targetPackageInput.value)
    }

    fun setTargetPackage(value: String) {
        _targetPackageInput.value = value
    }

    fun setTargetActivity(value: String) {
        _targetActivityInput.value = value
    }

    class Factory(
        private val permissionAuditor: PermissionAuditor,
        private val settingsRepository: SettingsRepository,
        private val appInstalledChecker: AppInstalledChecker,
        private val batteryOptimizationChecker: BatteryOptimizationChecker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(
                permissionAuditor,
                settingsRepository,
                appInstalledChecker,
                batteryOptimizationChecker
            ) as T
        }
    }
}

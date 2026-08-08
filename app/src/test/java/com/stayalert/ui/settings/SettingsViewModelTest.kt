package com.stayalert.ui.settings

import com.stayalert.data.AppInstalledChecker
import com.stayalert.data.BatteryOptimizationChecker
import com.stayalert.data.Permission
import com.stayalert.data.PermissionAuditor
import com.stayalert.data.PermissionState
import com.stayalert.data.PermissionStatus
import com.stayalert.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private class FakeSettingsRepository : SettingsRepository {
        private val _noticeAccepted = MutableStateFlow(false)
        private val _targetPackage = MutableStateFlow("com.microsoft.teams")
        private val _targetActivity = MutableStateFlow("com.microsoft.teams.activities.MainActivity")
        override val noticeAccepted: StateFlow<Boolean> = _noticeAccepted
        override val targetPackage: StateFlow<String> = _targetPackage
        override val targetActivity: StateFlow<String> = _targetActivity
        override suspend fun setNoticeAccepted(value: Boolean) { _noticeAccepted.value = value }
        override suspend fun setTargetPackage(value: String) { _targetPackage.value = value }
        override suspend fun setTargetActivity(value: String) { _targetActivity.value = value }
    }

    private class FakePermissionAuditor(
        private val overlayGranted: Boolean,
        private val notificationsGranted: Boolean
    ) : PermissionAuditor {
        override fun canDrawOverlays(): Boolean = overlayGranted
        override fun areNotificationsEnabled(): Boolean = notificationsGranted
        override fun audit(): List<PermissionStatus> = listOf(
            PermissionStatus(Permission.OVERLAY, if (overlayGranted) PermissionState.GRANTED else PermissionState.PENDING),
            PermissionStatus(Permission.NOTIFICATIONS, if (notificationsGranted) PermissionState.GRANTED else PermissionState.PENDING)
        )
    }

    private class FakeAppInstalledChecker(private val installed: Boolean) : AppInstalledChecker {
        override fun isInstalled(packageName: String): Boolean = installed
    }

    private class FakeBatteryOptimizationChecker(private val exempt: Boolean) : BatteryOptimizationChecker {
        override fun isIgnoringBatteryOptimizations(): Boolean = exempt
    }

    private fun createViewModel(
        overlayGranted: Boolean = true,
        notificationsGranted: Boolean = true,
        installed: Boolean = true,
        batteryExempt: Boolean = true
    ): SettingsViewModel = SettingsViewModel(
        permissionAuditor = FakePermissionAuditor(overlayGranted, notificationsGranted),
        settingsRepository = FakeSettingsRepository(),
        appInstalledChecker = FakeAppInstalledChecker(installed),
        batteryOptimizationChecker = FakeBatteryOptimizationChecker(batteryExempt)
    )

    @Test
    fun `refresh audita los permisos`() {
        val viewModel = createViewModel(overlayGranted = true, notificationsGranted = false)

        val expected = listOf(
            PermissionStatus(Permission.OVERLAY, PermissionState.GRANTED),
            PermissionStatus(Permission.NOTIFICATIONS, PermissionState.PENDING)
        )
        assertEquals(expected, viewModel.permissions.value)
    }

    @Test
    fun `refresh refleja el estado de bateria`() {
        val viewModel = createViewModel(batteryExempt = false)
        assertFalse(viewModel.batteryExempt.value)
    }

    @Test
    fun `refresh refleja la instalacion de la app objetivo`() {
        val viewModel = createViewModel(installed = false)
        assertFalse(viewModel.targetInstalled.value)
    }

    @Test
    fun `targetPackage expone el valor del repository`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val viewModel = createViewModel()
            assertEquals("com.microsoft.teams", viewModel.targetPackage.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `setTargetPackage actualiza el campo de texto`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val viewModel = createViewModel()
            viewModel.setTargetPackage("com.example.app")
            assertEquals("com.example.app", viewModel.targetPackage.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

package com.stayalert.ui.settings

import com.stayalert.data.Permission
import com.stayalert.data.PermissionAuditor
import com.stayalert.data.PermissionState
import com.stayalert.data.PermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

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

    @Test
    fun `refresh audita los permisos`() {
        val auditor = FakePermissionAuditor(overlayGranted = true, notificationsGranted = false)
        val viewModel = SettingsViewModel(auditor)

        val expected = listOf(
            PermissionStatus(Permission.OVERLAY, PermissionState.GRANTED),
            PermissionStatus(Permission.NOTIFICATIONS, PermissionState.PENDING)
        )
        assertEquals(expected, viewModel.permissions.value)
    }

    @Test
    fun `refresh re-audita tras cambios`() {
        val auditor = FakePermissionAuditor(overlayGranted = false, notificationsGranted = false)
        val viewModel = SettingsViewModel(auditor)
        assertEquals(PermissionState.PENDING, viewModel.permissions.value[0].state)

        val updated = FakePermissionAuditor(overlayGranted = true, notificationsGranted = false)
        val viewModel2 = SettingsViewModel(updated)
        assertEquals(PermissionState.GRANTED, viewModel2.permissions.value[0].state)
    }
}

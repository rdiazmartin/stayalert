package com.stayalert.domain

import com.stayalert.data.AppInstalledChecker
import com.stayalert.data.PermissionAuditor
import com.stayalert.data.PermissionStatus
import com.stayalert.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionValidatorTest {

    private class FakePermissionAuditor(
        private val overlay: Boolean,
        private val notifications: Boolean
    ) : PermissionAuditor {
        override fun canDrawOverlays(): Boolean = overlay
        override fun areNotificationsEnabled(): Boolean = notifications
        override fun audit(): List<PermissionStatus> = emptyList()
    }

    private class FakeSettingsRepository(
        private val noticeAcceptedValue: Boolean,
        private val targetPackageValue: String = "com.microsoft.teams"
    ) : SettingsRepository {
        override val noticeAccepted: Flow<Boolean> = MutableStateFlow(noticeAcceptedValue)
        override val targetPackage: Flow<String> = MutableStateFlow(targetPackageValue)
        override val targetActivity: Flow<String> = MutableStateFlow("")
        override suspend fun setNoticeAccepted(value: Boolean) {}
        override suspend fun setTargetPackage(value: String) {}
        override suspend fun setTargetActivity(value: String) {}
    }

    private class FakeAppInstalledChecker(private val installed: Boolean) : AppInstalledChecker {
        override fun isInstalled(packageName: String): Boolean = installed
    }

    @Test
    fun `validate devuelve null cuando todo esta listo`() = runTest {
        val validator = SessionValidator(
            FakePermissionAuditor(true, true),
            FakeSettingsRepository(true),
            FakeAppInstalledChecker(true)
        )
        assertNull(validator.validate())
    }

    @Test
    fun `validate devuelve PERMISSION_OVERLAY si falta el permiso de overlay`() = runTest {
        val validator = SessionValidator(
            FakePermissionAuditor(false, true),
            FakeSettingsRepository(true),
            FakeAppInstalledChecker(true)
        )
        assertEquals(ValidationFailure.PERMISSION_OVERLAY, validator.validate())
    }

    @Test
    fun `validate devuelve PERMISSION_NOTIFICATIONS si faltan las notificaciones`() = runTest {
        val validator = SessionValidator(
            FakePermissionAuditor(true, false),
            FakeSettingsRepository(true),
            FakeAppInstalledChecker(true)
        )
        assertEquals(ValidationFailure.PERMISSION_NOTIFICATIONS, validator.validate())
    }

    @Test
    fun `validate devuelve NOTICE_NOT_ACCEPTED si el aviso no fue aceptado`() = runTest {
        val validator = SessionValidator(
            FakePermissionAuditor(true, true),
            FakeSettingsRepository(false),
            FakeAppInstalledChecker(true)
        )
        assertEquals(ValidationFailure.NOTICE_NOT_ACCEPTED, validator.validate())
    }

    @Test
    fun `validate devuelve TARGET_NOT_INSTALLED si la app objetivo no esta instalada`() = runTest {
        val validator = SessionValidator(
            FakePermissionAuditor(true, true),
            FakeSettingsRepository(true),
            FakeAppInstalledChecker(false)
        )
        assertEquals(ValidationFailure.TARGET_NOT_INSTALLED, validator.validate())
    }
}

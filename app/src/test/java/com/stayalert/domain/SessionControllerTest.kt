package com.stayalert.domain

import com.stayalert.data.AppInstalledChecker
import com.stayalert.data.PermissionAuditor
import com.stayalert.data.PermissionStatus
import com.stayalert.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionControllerTest {

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

    private class Harness(scope: TestScope) {
        val commands = mutableListOf<SessionCommand>()
        val controller = SessionController(
            scope = scope.backgroundScope,
            validator = SessionValidator(
                FakePermissionAuditor(true, true),
                FakeSettingsRepository(true),
                FakeAppInstalledChecker(true)
            ),
            onCommand = { commands.add(it) }
        )
    }

    @Test
    fun `startSession transita a Lanzando y emite LaunchTarget`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        val failure = harness.controller.startSession()
        assertNull(failure)
        assertEquals(SessionState.Lanzando, harness.controller.state.value)
        assertEquals(listOf(SessionCommand.LaunchTarget), harness.commands)
    }

    @Test
    fun `startSession devuelve el motivo si la validacion falla`() = runTest(UnconfinedTestDispatcher()) {
        val controller = SessionController(
            scope = backgroundScope,
            validator = SessionValidator(
                FakePermissionAuditor(false, true),
                FakeSettingsRepository(true),
                FakeAppInstalledChecker(true)
            ),
            onCommand = {}
        )
        val failure = controller.startSession()
        assertEquals(ValidationFailure.PERMISSION_OVERLAY, failure)
        assertEquals(SessionState.Inactiva, controller.state.value)
    }

    @Test
    fun `OverlayShown transita a Aislada y emite StartFgs y StartWatchdog`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        harness.controller.startSession()
        harness.controller.emit(SessionEvent.OverlayShown)
        advanceUntilIdle()
        assertEquals(SessionState.Aislada, harness.controller.state.value)
        assertEquals(
            listOf(SessionCommand.LaunchTarget, SessionCommand.StartFgs, SessionCommand.StartWatchdog),
            harness.commands
        )
    }

    @Test
    fun `PatternDetected en Aislada termina la sesion`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        harness.controller.startSession()
        harness.controller.emit(SessionEvent.OverlayShown)
        advanceUntilIdle()
        harness.controller.emit(SessionEvent.PatternDetected)
        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, harness.controller.state.value)
        assertEquals(TerminationReason.Pattern, harness.controller.lastTerminationReason.value)
    }

    @Test
    fun `eventos de terminacion en Inactiva son no-ops`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        harness.controller.emit(SessionEvent.PatternDetected)
        harness.controller.emit(SessionEvent.ScreenOff)
        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, harness.controller.state.value)
        assertNull(harness.controller.lastTerminationReason.value)
    }

    @Test
    fun `LaunchFailed en Lanzando aborta a Inactiva`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        harness.controller.startSession()
        harness.controller.emit(SessionEvent.LaunchFailed("paquete no instalado"))
        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, harness.controller.state.value)
        assertEquals(TerminationReason.LaunchFailed, harness.controller.lastTerminationReason.value)
    }
}

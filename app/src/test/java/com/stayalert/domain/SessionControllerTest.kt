package com.stayalert.domain

import com.stayalert.data.AppInstalledChecker
import com.stayalert.data.PermissionAuditor
import com.stayalert.data.PermissionStatus
import com.stayalert.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
        val endedReasons = mutableListOf<TerminationReason>()
        val controller = SessionController(
            scope = scope.backgroundScope,
            validator = SessionValidator(
                FakePermissionAuditor(true, true),
                FakeSettingsRepository(true),
                FakeAppInstalledChecker(true)
            ),
            notifier = object : com.stayalert.data.Notifier {
                override fun createChannels() {}
                override fun showSessionNotification() {}
                override fun showSessionEnded(reason: TerminationReason) {
                    endedReasons.add(reason)
                }
                override fun cancelSessionNotification() {}
            },
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
            notifier = object : com.stayalert.data.Notifier {
                override fun createChannels() {}
                override fun showSessionNotification() {}
                override fun showSessionEnded(reason: TerminationReason) {}
                override fun cancelSessionNotification() {}
            },
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
    fun `PatternDetected en Aislada transita a Deteniendo y espera el hide del overlay`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        harness.controller.startSession()
        harness.controller.emit(SessionEvent.OverlayShown)
        advanceUntilIdle()
        harness.controller.emit(SessionEvent.PatternDetected)
        advanceUntilIdle()
        assertEquals(SessionState.Deteniendo, harness.controller.state.value)
        assertEquals(TerminationReason.Pattern, harness.controller.lastTerminationReason.value)

        harness.controller.emit(SessionEvent.OverlayHidden)
        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, harness.controller.state.value)
        assertEquals(listOf(TerminationReason.Pattern), harness.endedReasons)
    }

    @Test
    fun `OverlayHidden completa la terminacion y notifica el motivo`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        harness.controller.startSession()
        harness.controller.emit(SessionEvent.OverlayShown)
        advanceUntilIdle()
        harness.controller.emit(SessionEvent.StopRequested)
        advanceUntilIdle()
        assertEquals(SessionState.Deteniendo, harness.controller.state.value)

        harness.controller.emit(SessionEvent.OverlayHidden)
        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, harness.controller.state.value)
        assertEquals(listOf(TerminationReason.ManualStop), harness.endedReasons)
    }

    @Test
    fun `OverlayHideFailed completa la terminacion con log y motivo`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        harness.controller.startSession()
        harness.controller.emit(SessionEvent.OverlayShown)
        advanceUntilIdle()
        harness.controller.emit(SessionEvent.StopRequested)
        advanceUntilIdle()

        harness.controller.emit(SessionEvent.OverlayHideFailed("removeView lanzó"))
        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, harness.controller.state.value)
        assertEquals(listOf(TerminationReason.ManualStop), harness.endedReasons)
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
    fun `LaunchFailed en Lanzando aborta a Deteniendo y completa con OverlayHidden`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        harness.controller.startSession()
        harness.controller.emit(SessionEvent.LaunchFailed("paquete no instalado"))
        advanceUntilIdle()
        assertEquals(SessionState.Deteniendo, harness.controller.state.value)
        assertEquals(TerminationReason.LaunchFailed, harness.controller.lastTerminationReason.value)

        harness.controller.emit(SessionEvent.OverlayHidden)
        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, harness.controller.state.value)
        assertEquals(listOf(TerminationReason.LaunchFailed), harness.endedReasons)
    }

    @Test
    fun `terminacion idempotente en Deteniendo y sin notificacion duplicada`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        harness.controller.startSession()
        harness.controller.emit(SessionEvent.OverlayShown)
        advanceUntilIdle()
        harness.controller.emit(SessionEvent.PatternDetected)
        advanceUntilIdle()
        harness.controller.emit(SessionEvent.PatternDetected)
        harness.controller.emit(SessionEvent.ScreenOff)
        advanceUntilIdle()
        assertEquals(SessionState.Deteniendo, harness.controller.state.value)

        harness.controller.emit(SessionEvent.OverlayHidden)
        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, harness.controller.state.value)
        assertEquals(listOf(TerminationReason.Pattern), harness.endedReasons)
    }

    @Test
    fun `timeout de terminacion completa la sesion si el hide nunca llega`() = runTest(UnconfinedTestDispatcher()) {
        val harness = Harness(this)
        harness.controller.startSession()
        harness.controller.emit(SessionEvent.OverlayShown)
        advanceUntilIdle()
        harness.controller.emit(SessionEvent.StopRequested)
        advanceUntilIdle()
        assertEquals(SessionState.Deteniendo, harness.controller.state.value)

        advanceTimeBy(SessionController.TERMINATION_TIMEOUT_MS + 100)
        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, harness.controller.state.value)
        assertEquals(listOf(TerminationReason.ManualStop), harness.endedReasons)
    }
}

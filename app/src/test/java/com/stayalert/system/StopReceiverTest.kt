package com.stayalert.system

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.stayalert.data.AppInstalledChecker
import com.stayalert.data.PermissionAuditor
import com.stayalert.data.PermissionStatus
import com.stayalert.data.SettingsRepository
import com.stayalert.domain.SessionController
import com.stayalert.domain.SessionEvent
import com.stayalert.domain.SessionState
import com.stayalert.domain.SessionValidator
import com.stayalert.domain.TerminationReason
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StopReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private class FakePermissionAuditor : PermissionAuditor {
        override fun canDrawOverlays(): Boolean = true
        override fun areNotificationsEnabled(): Boolean = true
        override fun audit(): List<PermissionStatus> = emptyList()
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val noticeAccepted: Flow<Boolean> = MutableStateFlow(true)
        override val targetPackage: Flow<String> = MutableStateFlow("com.microsoft.teams")
        override val targetActivity: Flow<String> = MutableStateFlow("com.microsoft.teams.activities.MainActivity")
        override suspend fun setNoticeAccepted(value: Boolean) {}
        override suspend fun setTargetPackage(value: String) {}
        override suspend fun setTargetActivity(value: String) {}
    }

    private class FakeAppInstalledChecker : AppInstalledChecker {
        override fun isInstalled(packageName: String): Boolean = true
    }

    private fun createController(scope: kotlinx.coroutines.CoroutineScope): SessionController =
        SessionController(
            scope = scope,
            validator = SessionValidator(
                FakePermissionAuditor(),
                FakeSettingsRepository(),
                FakeAppInstalledChecker()
            ),
            onCommand = {}
        )

    private suspend fun kotlinx.coroutines.test.TestScope.startActiveSession(controller: SessionController) {
        controller.startSession()
        controller.emit(SessionEvent.OverlayShown)
        advanceUntilIdle()
        assertEquals(SessionState.Aislada, controller.state.value)
    }

    @Test
    fun `StopReceiver con sesion activa emite StopRequested y termina la sesion`() = runTest(UnconfinedTestDispatcher()) {
        val controller = createController(backgroundScope)
        startActiveSession(controller)

        val receiver = StopReceiver()
        receiver.sessionControllerProvider = { controller }
        receiver.onReceive(context, Intent(StopReceiver.ACTION_STOP_SESSION))

        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, controller.state.value)
        assertEquals(TerminationReason.ManualStop, controller.lastTerminationReason.value)
    }

    @Test
    fun `StopReceiver sin sesion activa es no-op`() = runTest(UnconfinedTestDispatcher()) {
        val controller = createController(backgroundScope)

        val receiver = StopReceiver()
        receiver.sessionControllerProvider = { controller }
        receiver.onReceive(context, Intent(StopReceiver.ACTION_STOP_SESSION))

        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, controller.state.value)
        assertNull(controller.lastTerminationReason.value)
    }

    @Test
    fun `StopReceiver no abre ninguna activity`() {
        val controller = createController(kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher()))

        val receiver = StopReceiver()
        receiver.sessionControllerProvider = { controller }
        receiver.onReceive(context, Intent(StopReceiver.ACTION_STOP_SESSION))

        val startedActivities = Shadows.shadowOf(context.applicationContext as android.app.Application).nextStartedActivity
        assertNull("StopReceiver no debe lanzar ninguna activity", startedActivities)
    }

    @Test
    fun `StopReceiver ignora acciones que no son STOP_SESSION`() = runTest(UnconfinedTestDispatcher()) {
        val controller = createController(backgroundScope)
        startActiveSession(controller)

        val receiver = StopReceiver()
        receiver.sessionControllerProvider = { controller }
        receiver.onReceive(context, Intent("otra.accion"))

        advanceUntilIdle()
        assertEquals(SessionState.Aislada, controller.state.value)
    }

    @Test
    fun `onDestroy del servicio con sesion activa termina la sesion con ServiceKilled`() = runTest(UnconfinedTestDispatcher()) {
        val controller = createController(backgroundScope)
        startActiveSession(controller)

        val serviceController: ServiceController<PresenceForegroundService> = Robolectric.buildService(PresenceForegroundService::class.java)
        serviceController.get().sessionControllerProvider = { controller }
        serviceController.create()
        serviceController.destroy()

        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, controller.state.value)
        assertEquals(TerminationReason.ServiceKilled, controller.lastTerminationReason.value)
    }

    @Test
    fun `onDestroy del servicio con sesion en Lanzando termina la sesion`() = runTest(UnconfinedTestDispatcher()) {
        val controller = createController(backgroundScope)
        controller.startSession()
        assertEquals(SessionState.Lanzando, controller.state.value)

        val serviceController: ServiceController<PresenceForegroundService> = Robolectric.buildService(PresenceForegroundService::class.java)
        serviceController.get().sessionControllerProvider = { controller }
        serviceController.create()
        serviceController.destroy()

        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, controller.state.value)
        assertEquals(TerminationReason.ServiceKilled, controller.lastTerminationReason.value)
    }

    @Test
    fun `onDestroy del servicio con sesion ya terminada no cambia el motivo`() = runTest(UnconfinedTestDispatcher()) {
        val controller = createController(backgroundScope)
        startActiveSession(controller)
        controller.emit(SessionEvent.PatternDetected)
        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, controller.state.value)
        assertEquals(TerminationReason.Pattern, controller.lastTerminationReason.value)

        val serviceController: ServiceController<PresenceForegroundService> = Robolectric.buildService(PresenceForegroundService::class.java)
        serviceController.get().sessionControllerProvider = { controller }
        serviceController.create()
        serviceController.destroy()

        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, controller.state.value)
        assertEquals(TerminationReason.Pattern, controller.lastTerminationReason.value)
    }

    @Test
    fun `onDestroy del servicio sin sesion no emite terminacion`() = runTest(UnconfinedTestDispatcher()) {
        val controller = createController(backgroundScope)

        val serviceController: ServiceController<PresenceForegroundService> = Robolectric.buildService(PresenceForegroundService::class.java)
        serviceController.get().sessionControllerProvider = { controller }
        serviceController.create()
        serviceController.destroy()

        advanceUntilIdle()
        assertEquals(SessionState.Inactiva, controller.state.value)
        assertNull(controller.lastTerminationReason.value)
    }
}

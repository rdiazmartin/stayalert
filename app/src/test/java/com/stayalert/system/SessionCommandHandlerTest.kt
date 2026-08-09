package com.stayalert.system

import com.stayalert.data.SettingsRepository
import com.stayalert.domain.ForegroundMonitor
import com.stayalert.domain.ForegroundStatus
import com.stayalert.domain.LaunchError
import com.stayalert.domain.OverlayController
import com.stayalert.domain.SessionCommand
import com.stayalert.domain.SessionConstants
import com.stayalert.domain.SessionEvent
import com.stayalert.domain.TargetApp
import com.stayalert.domain.TargetAppLauncher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionCommandHandlerTest {

    private class FakeSettingsRepository : SettingsRepository {
        override val noticeAccepted: Flow<Boolean> = MutableStateFlow(true)
        override val targetPackage: Flow<String> = MutableStateFlow("com.microsoft.teams")
        override val targetActivity: Flow<String> = MutableStateFlow("com.microsoft.teams.activities.MainActivity")
        override suspend fun setNoticeAccepted(value: Boolean) {}
        override suspend fun setTargetPackage(value: String) {}
        override suspend fun setTargetActivity(value: String) {}
    }

    private class FakeLauncher(private val success: Boolean) : TargetAppLauncher {
        override suspend fun launch(target: TargetApp): Result<Unit> =
            if (success) Result.success(Unit) else Result.failure(LaunchError.PackageNotInstalled)
    }

    private class FakeMonitor(private val status: ForegroundStatus) : ForegroundMonitor {
        override suspend fun status(packageName: String): ForegroundStatus = status
    }

    private class FakeOverlay(private val onEvent: (SessionEvent) -> Unit) : OverlayController {
        var shown = false
        override fun show() {
            shown = true
            onEvent(SessionEvent.OverlayShown)
        }
        override fun hide() { shown = false }
        override fun isVisible(): Boolean = shown
    }

    private fun createHandler(
        scope: kotlinx.coroutines.CoroutineScope,
        events: MutableList<SessionEvent>,
        overlay: FakeOverlay,
        launcherSuccess: Boolean = true,
        monitorStatus: ForegroundStatus = ForegroundStatus.FOREGROUND
    ): SessionCommandHandler = SessionCommandHandler(
        context = androidx.test.core.app.ApplicationProvider.getApplicationContext(),
        scope = scope,
        settingsRepository = FakeSettingsRepository(),
        targetAppLauncher = FakeLauncher(launcherSuccess),
        foregroundMonitor = FakeMonitor(monitorStatus),
        overlayController = overlay,
        notifier = object : com.stayalert.data.Notifier {
            override fun createChannels() {}
            override fun showSessionNotification() {}
            override fun showSessionEnded(reason: com.stayalert.domain.TerminationReason) {}
        },
        onEvent = { events.add(it) }
    )

    @Test
    fun `launch exitoso despliega el overlay tras el delay`() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<SessionEvent>()
        val overlay = FakeOverlay { events.add(it) }
        val handler = createHandler(backgroundScope, events, overlay)

        handler.handle(SessionCommand.LaunchTarget)
        advanceTimeBy(SessionConstants.LAUNCH_DELAY_MS + 100)
        assertTrue(overlay.shown)
        assertTrue(events.contains(SessionEvent.OverlayShown))
    }

    @Test
    fun `launch fallido emite LaunchFailed sin overlay`() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<SessionEvent>()
        val overlay = FakeOverlay { events.add(it) }
        val handler = createHandler(backgroundScope, events, overlay, launcherSuccess = false)

        handler.handle(SessionCommand.LaunchTarget)
        advanceTimeBy(SessionConstants.LAUNCH_DELAY_MS + 100)
        assertTrue(events.any { it is SessionEvent.LaunchFailed })
        assertEquals(false, overlay.shown)
    }

    @Test
    fun `app objetivo no en primer plano aborta sin overlay`() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<SessionEvent>()
        val overlay = FakeOverlay { events.add(it) }
        val handler = createHandler(backgroundScope, events, overlay, monitorStatus = ForegroundStatus.NOT_FOREGROUND)

        handler.handle(SessionCommand.LaunchTarget)
        advanceTimeBy(SessionConstants.LAUNCH_DELAY_MS + 100)
        assertTrue(events.any { it is SessionEvent.LaunchFailed })
        assertEquals(false, overlay.shown)
    }
}

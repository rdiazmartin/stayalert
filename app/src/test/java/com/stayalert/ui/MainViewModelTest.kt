package com.stayalert.ui.viewmodel

import com.stayalert.data.SettingsRepository
import com.stayalert.domain.SessionController
import com.stayalert.domain.SessionValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private class FakeSettingsRepository(initial: Boolean = false) : SettingsRepository {
        private val _noticeAccepted = MutableStateFlow(initial)
        private val _targetPackage = MutableStateFlow("com.microsoft.teams")
        private val _targetActivity = MutableStateFlow("com.microsoft.teams.activities.MainActivity")
        override val noticeAccepted: StateFlow<Boolean> = _noticeAccepted
        override val targetPackage: StateFlow<String> = _targetPackage
        override val targetActivity: StateFlow<String> = _targetActivity

        override suspend fun setNoticeAccepted(value: Boolean) {
            _noticeAccepted.value = value
        }

        override suspend fun setTargetPackage(value: String) {
            _targetPackage.value = value
        }

        override suspend fun setTargetActivity(value: String) {
            _targetActivity.value = value
        }
    }

    private fun createViewModel(
        repository: SettingsRepository = FakeSettingsRepository(false),
        controller: SessionController = SessionController(
            scope = kotlinx.coroutines.test.TestScope(),
            validator = SessionValidator(
                permissionAuditor = object : com.stayalert.data.PermissionAuditor {
                    override fun canDrawOverlays(): Boolean = true
                    override fun areNotificationsEnabled(): Boolean = true
                    override fun audit(): List<com.stayalert.data.PermissionStatus> = emptyList()
                },
                settingsRepository = repository,
                appInstalledChecker = object : com.stayalert.data.AppInstalledChecker {
                    override fun isInstalled(packageName: String): Boolean = true
                }
            ),
            notifier = object : com.stayalert.data.Notifier {
                override fun createChannels() {}
                override fun showSessionNotification() {}
                override fun showSessionEnded(reason: com.stayalert.domain.TerminationReason) {}
                override fun cancelSessionNotification() {}
            },
            onCommand = {}
        )
    ): MainViewModel = MainViewModel(repository, controller)

    @Test
    fun `noticeAccepted expone el valor inicial del repository`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val viewModel = createViewModel()
            assertFalse(viewModel.noticeAccepted.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `acceptNotice propaga la aceptacion al repository`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val fakeRepository = FakeSettingsRepository(false)
            val viewModel = createViewModel(repository = fakeRepository)
            viewModel.acceptNotice()
            assertTrue(viewModel.noticeAccepted.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

package com.stayalert.ui

import android.annotation.SuppressLint
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.stayalert.data.SettingsRepository
import com.stayalert.ui.theme.StayAlertTheme
import com.stayalert.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class MainScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeSettingsRepository(initial: Boolean = false) : SettingsRepository {
        private val _noticeAccepted = MutableStateFlow(initial)
        override val noticeAccepted: StateFlow<Boolean> = _noticeAccepted

        override suspend fun setNoticeAccepted(value: Boolean) {
            _noticeAccepted.value = value
        }
    }

    @SuppressLint("ViewModelConstructorInComposable")
    @Test
    fun `modal visible cuando aviso no aceptado y boton deshabilitado`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            composeRule.setContent {
                StayAlertTheme {
                    MainScreen(viewModel = MainViewModel(FakeSettingsRepository(false)))
                }
            }

            composeRule.onNodeWithText("Aviso de uso responsable").assertIsDisplayed()
            composeRule.onNodeWithText("Iniciar Jornada").assertIsNotEnabled()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @SuppressLint("ViewModelConstructorInComposable")
    @Test
    fun `aceptar oculta modal y habilita boton`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val fakeRepository = FakeSettingsRepository(false)
            composeRule.setContent {
                StayAlertTheme {
                    MainScreen(viewModel = MainViewModel(fakeRepository))
                }
            }

            composeRule.onNodeWithText("Entiendo y acepto").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Aviso de uso responsable").assertDoesNotExist()
            composeRule.onNodeWithText("Iniciar Jornada").assertIsEnabled()
        } finally {
            Dispatchers.resetMain()
        }
    }
}

package com.stayalert.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    private fun createRepository(): SettingsRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.preferencesDataStoreFile("test_settings_${System.nanoTime()}")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        return DataStoreSettingsRepository(context, dataStore)
    }

    @Test
    fun `noticeAccepted es false por defecto`() = runTest {
        val repository = createRepository()
        assertFalse(repository.noticeAccepted.first())
    }

    @Test
    fun `setNoticeAccepted true persiste el valor`() = runTest {
        val repository = createRepository()
        repository.setNoticeAccepted(true)
        assertTrue(repository.noticeAccepted.first())
    }

    @Test
    fun `targetPackage usa el default de Teams`() = runTest {
        val repository = createRepository()
        assertEquals(DEFAULT_TARGET_PACKAGE, repository.targetPackage.first())
    }

    @Test
    fun `setTargetPackage persiste el valor`() = runTest {
        val repository = createRepository()
        repository.setTargetPackage("com.example.app")
        assertEquals("com.example.app", repository.targetPackage.first())
    }

    @Test
    fun `setTargetActivity persiste el valor`() = runTest {
        val repository = createRepository()
        repository.setTargetActivity("com.example.app.MainActivity")
        assertEquals("com.example.app.MainActivity", repository.targetActivity.first())
    }
}

package com.stayalert.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
}

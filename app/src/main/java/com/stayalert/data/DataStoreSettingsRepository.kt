package com.stayalert.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.applicationContext.dataStore
) : SettingsRepository {

    override val noticeAccepted: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.NOTICE_ACCEPTED] ?: false
        }

    override suspend fun setNoticeAccepted(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.NOTICE_ACCEPTED] = value
        }
    }
}

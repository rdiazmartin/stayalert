package com.stayalert.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

const val DEFAULT_TARGET_PACKAGE = "com.microsoft.teams"
const val DEFAULT_TARGET_ACTIVITY = "com.microsoft.teams.activities.MainActivity"

class DataStoreSettingsRepository(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.applicationContext.dataStore
) : SettingsRepository {

    override val noticeAccepted: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.NOTICE_ACCEPTED] ?: false
        }

    override val targetPackage: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.TARGET_PACKAGE] ?: DEFAULT_TARGET_PACKAGE
        }

    override val targetActivity: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.TARGET_ACTIVITY] ?: DEFAULT_TARGET_ACTIVITY
        }

    override suspend fun setNoticeAccepted(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.NOTICE_ACCEPTED] = value
        }
    }

    override suspend fun setTargetPackage(value: String) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.TARGET_PACKAGE] = value
        }
    }

    override suspend fun setTargetActivity(value: String) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.TARGET_ACTIVITY] = value
        }
    }
}

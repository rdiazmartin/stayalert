package com.stayalert.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val NOTICE_ACCEPTED = booleanPreferencesKey("notice_accepted")
    val TARGET_PACKAGE = stringPreferencesKey("target_package")
    val TARGET_ACTIVITY = stringPreferencesKey("target_activity")
}

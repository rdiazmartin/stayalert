package com.stayalert.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val noticeAccepted: Flow<Boolean>
    suspend fun setNoticeAccepted(value: Boolean)
}

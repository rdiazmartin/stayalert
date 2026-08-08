package com.stayalert.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val noticeAccepted: Flow<Boolean>
    val targetPackage: Flow<String>
    val targetActivity: Flow<String>
    suspend fun setNoticeAccepted(value: Boolean)
    suspend fun setTargetPackage(value: String)
    suspend fun setTargetActivity(value: String)
}

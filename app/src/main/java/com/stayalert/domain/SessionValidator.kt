package com.stayalert.domain

import com.stayalert.data.AppInstalledChecker
import com.stayalert.data.PermissionAuditor
import com.stayalert.data.SettingsRepository
import kotlinx.coroutines.flow.first

class SessionValidator(
    private val permissionAuditor: PermissionAuditor,
    private val settingsRepository: SettingsRepository,
    private val appInstalledChecker: AppInstalledChecker
) {

    suspend fun validate(): ValidationFailure? {
        if (!permissionAuditor.canDrawOverlays()) return ValidationFailure.PERMISSION_OVERLAY
        if (!permissionAuditor.areNotificationsEnabled()) return ValidationFailure.PERMISSION_NOTIFICATIONS
        if (!settingsRepository.noticeAccepted.first()) return ValidationFailure.NOTICE_NOT_ACCEPTED
        val targetPackage = settingsRepository.targetPackage.first()
        if (!appInstalledChecker.isInstalled(targetPackage)) return ValidationFailure.TARGET_NOT_INSTALLED
        return null
    }
}

package com.stayalert.data

import android.content.Context
import android.content.pm.PackageManager

class SystemAppInstalledChecker(private val context: Context) : AppInstalledChecker {

    override fun isInstalled(packageName: String): Boolean =
        try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            info.enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
}

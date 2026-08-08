package com.stayalert.data

interface AppInstalledChecker {
    fun isInstalled(packageName: String): Boolean
}

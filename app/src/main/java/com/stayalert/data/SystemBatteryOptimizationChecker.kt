package com.stayalert.data

import android.content.Context
import android.os.PowerManager

class SystemBatteryOptimizationChecker(private val context: Context) : BatteryOptimizationChecker {

    override fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}

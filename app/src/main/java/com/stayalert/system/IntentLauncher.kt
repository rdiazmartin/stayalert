package com.stayalert.system

import android.content.Context
import android.content.Intent
import com.stayalert.domain.LaunchError
import com.stayalert.domain.TargetApp
import com.stayalert.domain.TargetAppLauncher

class IntentLauncher(private val context: Context) : TargetAppLauncher {

    override suspend fun launch(target: TargetApp): Result<Unit> {
        val intent = Intent(Intent.ACTION_MAIN)
            .setClassName(target.packageName, target.activityName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo == null) {
                Result.failure(LaunchError.ActivityNotFound)
            } else {
                context.startActivity(intent)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(LaunchError.SystemFailure(e))
        }
    }
}

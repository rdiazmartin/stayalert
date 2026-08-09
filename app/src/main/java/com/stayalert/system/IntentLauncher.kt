package com.stayalert.system

import android.content.Context
import android.content.Intent
import com.stayalert.domain.LaunchError
import com.stayalert.domain.TargetApp
import com.stayalert.domain.TargetAppLauncher

class IntentLauncher(private val context: Context) : TargetAppLauncher {

    override suspend fun launch(target: TargetApp): Result<Unit> {
        val explicit = Intent(Intent.ACTION_MAIN)
            .setClassName(target.packageName, target.activityName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            if (context.packageManager.resolveActivity(explicit, 0) != null) {
                context.startActivity(explicit)
                return Result.success(Unit)
            }
            val launchIntent = context.packageManager.getLaunchIntentForPackage(target.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Result.success(Unit)
            } else {
                Result.failure(LaunchError.ActivityNotFound)
            }
        } catch (e: Exception) {
            Result.failure(LaunchError.SystemFailure(e))
        }
    }
}

package com.stayalert.domain

interface TargetAppLauncher {
    suspend fun launch(target: TargetApp): Result<Unit>
}

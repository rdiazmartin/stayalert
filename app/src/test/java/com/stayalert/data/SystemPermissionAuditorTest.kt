package com.stayalert.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemPermissionAuditorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `canDrawOverlays refleja el estado del sistema`() {
        val auditor = SystemPermissionAuditor(context)
        assertFalse(auditor.canDrawOverlays())
    }

    @Test
    fun `areNotificationsEnabled refleja el estado del sistema`() {
        val auditor = SystemPermissionAuditor(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        assertEquals(manager.areNotificationsEnabled(), auditor.areNotificationsEnabled())
    }

    @Test
    fun `audit devuelve ambos permisos`() {
        val auditor = SystemPermissionAuditor(context)
        val result = auditor.audit()

        assertEquals(2, result.size)
        assertEquals(Permission.OVERLAY, result[0].permission)
        assertEquals(Permission.NOTIFICATIONS, result[1].permission)
    }
}

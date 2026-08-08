package com.stayalert

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTokensTest {

    @Test
    fun `tokens de color coinciden con los valores del DESIGN md`() {
        assertEquals(Color(0xFF121212), com.stayalert.ui.theme.SurfaceBase)
        assertEquals(Color(0xFF1E1E1E), com.stayalert.ui.theme.SurfaceRaised)
        assertEquals(Color(0xFF000000), com.stayalert.ui.theme.SurfaceOverlay)
        assertEquals(Color(0xFFE6E6E6), com.stayalert.ui.theme.InkPrimary)
        assertEquals(Color(0xFF9E9E9E), com.stayalert.ui.theme.InkSecondary)
        assertEquals(Color(0xFF616161), com.stayalert.ui.theme.InkDisabled)
        assertEquals(Color(0xFF4CAF50), com.stayalert.ui.theme.Accent)
        assertEquals(Color(0xFF0B3D0F), com.stayalert.ui.theme.AccentOn)
        assertEquals(Color(0xFF2C2C2C), com.stayalert.ui.theme.BorderHairline)
        assertEquals(Color(0xFFCF6679), com.stayalert.ui.theme.Error)
        assertEquals(Color(0xFFF0A020), com.stayalert.ui.theme.Warning)
    }
}

package com.stayalert.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Shapes — DESIGN.md: sm 8dp, md 16dp, full 999dp (píldora solo para el botón principal)
val StayAlertShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(999.dp)
)

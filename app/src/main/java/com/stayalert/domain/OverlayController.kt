package com.stayalert.domain

interface OverlayController {
    fun show()
    suspend fun hide()
    fun isVisible(): Boolean
}

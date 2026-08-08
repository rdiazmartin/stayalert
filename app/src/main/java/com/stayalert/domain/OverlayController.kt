package com.stayalert.domain

interface OverlayController {
    fun show()
    fun hide()
    fun isVisible(): Boolean
}

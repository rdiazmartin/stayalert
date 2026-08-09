package com.stayalert.data

import com.stayalert.domain.TerminationReason

interface Notifier {
    fun createChannels()
    fun showSessionNotification()
    fun showSessionEnded(reason: TerminationReason)
}

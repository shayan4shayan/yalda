package com.shdarv.yalda.platform

import androidx.compose.runtime.Composable

interface NotificationScheduler {
    val isSupported: Boolean
    fun hasPermission(): Boolean
    fun requestPermission()
    fun scheduleDailyWord(profileId: Long, schedule: NotificationSchedule, enabled: Boolean)
    fun sendSample(profileId: Long)
}

@Composable
expect fun rememberNotificationScheduler(): NotificationScheduler

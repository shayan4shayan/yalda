package com.shdarv.yalda.platform

data class NotificationTime(val hour: Int, val minute: Int)

data class NotificationRange(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val intervalMinutes: Int
)

enum class NotificationStrategy {
    ExactTimes,
    TimeRanges
}

data class NotificationSchedule(
    val strategy: NotificationStrategy,
    val times: List<NotificationTime>,
    val ranges: List<NotificationRange>
)

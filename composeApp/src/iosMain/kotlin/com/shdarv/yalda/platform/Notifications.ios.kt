package com.shdarv.yalda.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.shdarv.yalda.db.database
import com.shdarv.yalda.db.init
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.UserNotifications.*
import platform.darwin.NSObject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val WORK_ID_PREFIX = "daily_word_"
private const val DAYS_AHEAD = 7

private class ForegroundNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionAlert or UNNotificationPresentationOptionSound
        )
    }
}

private class IosNotificationScheduler : NotificationScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val delegate = ForegroundNotificationDelegate()
    private var authorized: Boolean = false

    init {
        center.delegate = delegate
        refreshPermission()
    }

    override val isSupported: Boolean = true

    override fun hasPermission(): Boolean = authorized

    override fun requestPermission() {
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { granted, _ ->
            if (granted != null) {
                authorized = granted
            }
        }
    }

    override fun scheduleDailyWord(profileId: Long, schedule: NotificationSchedule, enabled: Boolean) {
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val ids = requests
                ?.filterIsInstance<UNNotificationRequest>()
                ?.mapNotNull { request ->
                    (request as? UNNotificationRequest)?.identifier?.takeIf { it.startsWith(WORK_ID_PREFIX + profileId) }
                }
                .orEmpty()
            if (ids.isNotEmpty()) {
                center.removePendingNotificationRequestsWithIdentifiers(ids)
            }
        }
        if (!enabled) return

        CoroutineScope(Dispatchers.Default).launch {
            if (!database.isInitialized()) {
                database.init()
            }
            val words = database.get().wordEntryDao().getWordsForProfile(profileId)
            if (words.isEmpty()) return@launch
            val calendar = NSCalendar.currentCalendar
            val now = NSDate()

            when (schedule.strategy) {
                NotificationStrategy.ExactTimes -> {
                    if (schedule.times.isEmpty()) return@launch
                    for (offset in 0 until DAYS_AHEAD) {
                        val date = calendar.dateByAddingUnit(
                            unit = NSCalendarUnitDay, value = offset.toLong(), toDate = now, options = 0u
                        ) ?: continue
                        schedule.times.forEach { time ->
                            val components = calendar.components(
                                NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = date
                            ).apply {
                                this.hour = time.hour.toLong()
                                this.minute = time.minute.toLong()
                            }
                            val word = words.random()
                            val content = UNMutableNotificationContent().apply {
                                setTitle(word.word)
                                setBody(word.meaning)
                                setSound(UNNotificationSound.defaultSound)
                            }
                            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                                dateComponents = components, repeats = false
                            )
                            val identifier =
                                "$WORK_ID_PREFIX${profileId}_time_${time.hour}_${time.minute}_${components.year}_${components.month}_${components.day}"
                            val request = UNNotificationRequest.requestWithIdentifier(
                                identifier = identifier, content = content, trigger = trigger
                            )
                            center.addNotificationRequest(request, null)
                        }
                    }
                }
                NotificationStrategy.TimeRanges -> {
                    if (schedule.ranges.isEmpty()) return@launch
                    for (offset in 0 until DAYS_AHEAD) {
                        val date = calendar.dateByAddingUnit(
                            unit = NSCalendarUnitDay, value = offset.toLong(), toDate = now, options = 0u
                        ) ?: continue
                        schedule.ranges.forEach { range ->
                            val startMinutes = range.startHour * 60 + range.startMinute
                            val endMinutes = range.endHour * 60 + range.endMinute
                            val intervalMinutes = range.intervalMinutes.coerceAtLeast(1)
                            if (endMinutes <= startMinutes) return@forEach
                            var minuteOfDay = startMinutes
                            while (minuteOfDay <= endMinutes) {
                                val components = calendar.components(
                                    NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = date
                                ).apply {
                                    this.hour = (minuteOfDay / 60).toLong()
                                    this.minute = (minuteOfDay % 60).toLong()
                                }
                                val word = words.random()
                                val content = UNMutableNotificationContent().apply {
                                    setTitle(word.word)
                                    setBody(word.meaning)
                                    setSound(UNNotificationSound.defaultSound)
                                }
                                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                                    dateComponents = components, repeats = false
                                )
                                val identifier =
                                    "$WORK_ID_PREFIX${profileId}_range_${range.startHour}_${range.startMinute}_${range.endHour}_${range.endMinute}_${range.intervalMinutes}_${components.year}_${components.month}_${components.day}_${minuteOfDay}"
                                val request = UNNotificationRequest.requestWithIdentifier(
                                    identifier = identifier, content = content, trigger = trigger
                                )
                                center.addNotificationRequest(request, null)
                                minuteOfDay += intervalMinutes
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun sendSample(profileId: Long) {
        CoroutineScope(Dispatchers.Default).launch {
            if (!database.isInitialized()) {
                database.init()
            }
            val word = database.get().wordEntryDao().getWordsForProfile(profileId).randomOrNull()
            val content = UNMutableNotificationContent()
            content.setTitle(word?.word ?: "Sample word")
            content.setBody(word?.meaning ?: "Open Yalda to learn a new word.")
            content.setSound(UNNotificationSound.defaultSound)
            val trigger =
                UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, repeats = false)
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "sample_${profileId}_${Clock.System.now().epochSeconds}",
                content = content,
                trigger = trigger
            )
            center.addNotificationRequest(request, null)
        }
    }

    private fun refreshPermission() {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            authorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
        }
    }
}

@Composable
actual fun rememberNotificationScheduler(): NotificationScheduler =
    remember { IosNotificationScheduler() }

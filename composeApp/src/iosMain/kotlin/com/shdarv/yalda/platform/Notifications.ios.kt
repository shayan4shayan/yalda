package com.shdarv.yalda.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.shdarv.yalda.db.*
import com.shdarv.yalda.audit.wordAlertEvent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import com.shdarv.yalda.db.database
import com.shdarv.yalda.db.init
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSDate
import platform.UserNotifications.*
import platform.darwin.NSObject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val WORK_ID_PREFIX = "daily_word_"
private const val DAYS_AHEAD = 7

private val notificationAuditScope = CoroutineScope(Dispatchers.Default)

private fun recordObservedNotification(notification: UNNotification, opened: Boolean = false) {
    val auditId = notification.request.content.userInfo["auditId"] as? String ?: return
    notificationAuditScope.launch {
        if (!database.isInitialized()) database.init()
        val dao = database.get().auditDao()
        val original = dao.eventByKey("$auditId:${AuditAction.ALERT_ATTEMPTED}") ?: return@launch
        dao.append(original.copy(
            id = 0, eventKey = "$auditId:${AuditAction.ALERT_DELIVERED}",
            action = AuditAction.ALERT_DELIVERED,
            occurredAt = (notification.date.timeIntervalSince1970 * 1000).toLong()
        ))
        if (opened) dao.append(original.copy(
            id = 0, eventKey = "$auditId:${AuditAction.ALERT_OPENED}",
            action = AuditAction.ALERT_OPENED, occurredAt = auditNow()
        ))
    }
}

private class ForegroundNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        recordObservedNotification(willPresentNotification)
        withCompletionHandler(
            UNNotificationPresentationOptionAlert or UNNotificationPresentationOptionSound
        )
    }
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        recordObservedNotification(didReceiveNotificationResponse.notification, opened = true)
        withCompletionHandler()
    }

}

private class IosNotificationScheduler : NotificationScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val delegate = ForegroundNotificationDelegate()
    private var authorized: Boolean = false
    private val activationObserver = NSNotificationCenter.defaultCenter.addObserverForName(
        UIApplicationDidBecomeActiveNotification, null, null
    ) { reconcileDeliveredNotifications() }

    private fun reconcileDeliveredNotifications() {
        center.getDeliveredNotificationsWithCompletionHandler { notifications ->
            notifications?.filterIsInstance<UNNotification>()?.forEach { recordObservedNotification(it) }
        }
    }

    init {
        center.delegate = delegate
        refreshPermission()
        reconcileDeliveredNotifications()
    }

    override val isSupported: Boolean = true

    override fun hasPermission(): Boolean = authorized

    override fun requestPermission() {
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { granted, _ ->
            authorized = granted
        }
    }

    override fun scheduleDailyWord(profileId: Long, schedule: NotificationSchedule, enabled: Boolean) {
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val ids = requests
                ?.filterIsInstance<UNNotificationRequest>()
                ?.mapNotNull { request ->
                    (request as? UNNotificationRequest)?.identifier?.takeIf { it.startsWith("$WORK_ID_PREFIX${profileId}_") }
                }
                .orEmpty()
            if (ids.isNotEmpty()) {
                center.removePendingNotificationRequestsWithIdentifiers(ids)
                notificationAuditScope.launch {
                    if (!database.isInitialized()) database.init()
                    requests?.filterIsInstance<UNNotificationRequest>()?.filter { it.identifier in ids }?.forEach { request ->
                        val auditId = request.content.userInfo["auditId"] as? String ?: return@forEach
                        val dao = database.get().auditDao()
                        val original = dao.eventByKey("$auditId:${AuditAction.ALERT_ATTEMPTED}") ?: return@forEach
                        dao.append(original.copy(id = 0, eventKey = "$auditId:${AuditAction.ALERT_CANCELED}",
                            action = AuditAction.ALERT_CANCELED, occurredAt = auditNow()))
                    }
                }
            }
            if (enabled) {
                schedulePendingNotifications(profileId, schedule)
            }
        }
    }

    private fun schedulePendingNotifications(profileId: Long, schedule: NotificationSchedule) {
        CoroutineScope(Dispatchers.Default).launch {
            if (!database.isInitialized()) {
                database.init()
            }
            val words = database.get().wordEntryDao().getWordsForProfile(profileId)
            if (words.isEmpty()) return@launch
            val calendar = NSCalendar.currentCalendar
            val now = NSDate()
            val nowComponents = calendar.components(
                NSCalendarUnitHour or NSCalendarUnitMinute,
                fromDate = now
            )
            val nowMinutes = nowComponents.hour.toInt() * 60 + nowComponents.minute.toInt()

            when (schedule.strategy) {
                NotificationStrategy.ExactTimes -> {
                    if (schedule.times.isEmpty()) return@launch
                    for (offset in 0 until DAYS_AHEAD) {
                        val date = calendar.dateByAddingUnit(
                            unit = NSCalendarUnitDay, value = offset.toLong(), toDate = now, options = 0u
                        ) ?: continue
                        schedule.times.forEach { time ->
                            val timeMinutes = time.hour * 60 + time.minute
                            if (offset == 0 && timeMinutes <= nowMinutes) return@forEach

                            val components = calendar.components(
                                NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = date
                            ).apply {
                                this.hour = time.hour.toLong()
                                this.minute = time.minute.toLong()
                            }
                            val word = words.random()
                            val content = UNMutableNotificationContent().apply {
                                setUserInfo(mapOf("auditId" to newAuditId()))
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
                            scheduleAudited(request, word, profileId)
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
                            if (offset == 0) {
                                if (nowMinutes >= endMinutes) return@forEach
                                while (minuteOfDay <= nowMinutes) {
                                    minuteOfDay += intervalMinutes
                                }
                            }
                            while (minuteOfDay <= endMinutes) {
                                val components = calendar.components(
                                    NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = date
                                ).apply {
                                    this.hour = (minuteOfDay / 60).toLong()
                                    this.minute = (minuteOfDay % 60).toLong()
                                }
                                val word = words.random()
                                val content = UNMutableNotificationContent().apply {
                                    setUserInfo(mapOf("auditId" to newAuditId()))
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
                                scheduleAudited(request, word, profileId)
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
            content.setUserInfo(mapOf("auditId" to newAuditId()))
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
            scheduleAudited(request, word, profileId, isSample = true)
        }
    }

    private suspend fun scheduleAudited(
        request: UNNotificationRequest, word: WordEntry?, profileId: Long, isSample: Boolean = false
    ) {
        val auditId = request.content.userInfo["auditId"] as String
        val trigger = request.trigger
        val scheduledAt = when (trigger) {
            is UNCalendarNotificationTrigger -> trigger.nextTriggerDate()
            is UNTimeIntervalNotificationTrigger -> trigger.nextTriggerDate()
            else -> null
        }?.let { (it.timeIntervalSince1970 * 1000).toLong() }
        val attempted = if (word != null) {
            wordAlertEvent(AuditAction.ALERT_ATTEMPTED, word, auditId, isSample, scheduledAt)
        } else AuditEvent(
            eventKey = "$auditId:${AuditAction.ALERT_ATTEMPTED}", action = AuditAction.ALERT_ATTEMPTED,
            profileId = profileId, notificationId = auditId, isSample = isSample, scheduledAt = scheduledAt
        )
        val dao = database.get().auditDao()
        dao.append(attempted)
        val blocked: Boolean = suspendCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings?.authorizationStatus == UNAuthorizationStatusDenied ||
                    settings?.authorizationStatus == UNAuthorizationStatusNotDetermined)
            }
        }
        if (blocked) {
            dao.append(attempted.copy(
                eventKey = "$auditId:${AuditAction.ALERT_BLOCKED}", action = AuditAction.ALERT_BLOCKED,
                occurredAt = auditNow(), detailsJson = "{\"reason\":\"Notification permission unavailable\"}"
            ))
            return
        }
        val failure: String? = suspendCoroutine { continuation ->
            center.addNotificationRequest(request) { error -> continuation.resume(error?.localizedDescription) }
        }
        val action = if (failure == null) AuditAction.ALERT_SCHEDULED else AuditAction.ALERT_FAILED
        dao.append(attempted.copy(
            eventKey = "$auditId:$action", action = action, occurredAt = auditNow(),
            detailsJson = failure?.let { buildJsonObject { put("reason", it) }.toString() }
        ))
    }

    private fun refreshPermission() {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            authorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
        }
    }
}

private val iosNotificationScheduler by lazy { IosNotificationScheduler() }

internal fun initializeNotificationAudit() { iosNotificationScheduler }

@Composable
actual fun rememberNotificationScheduler(): NotificationScheduler =
    remember { iosNotificationScheduler }

package com.shdarv.yalda.platform

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shdarv.yalda.R
import com.shdarv.yalda.db.database
import com.shdarv.yalda.db.getDatabaseBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val CHANNEL_ID = "yalda_daily_words"
private const val CHANNEL_NAME = "Daily Words"
private const val CHANNEL_DESC = "Daily reminder with a word and its meaning."
private const val LEGACY_WORK_TAG_PREFIX = "daily_word_tag_"
private const val REMINDER_PREFS = "yalda.reminder_schedules"
private const val PROFILE_IDS_KEY = "profile_ids"
private const val ACTION_DAILY_WORD_REMINDER = "com.shdarv.yalda.action.DAILY_WORD_REMINDER"
private const val DAY_MINUTES = 24 * 60
internal const val KEY_PROFILE_ID = "profile_id"

private class AndroidNotificationScheduler(private val context: Context) : NotificationScheduler {
    override val isSupported: Boolean = true

    override fun hasPermission(): Boolean {
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return hasNotificationPermission && canScheduleExactReminderAlarms(context)
    }

    override fun requestPermission() {
        val activity = context as? Activity ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1002
            )
            return
        }

        if (!canScheduleExactReminderAlarms(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
            activity.startActivity(intent)
        }
    }

    override fun scheduleDailyWord(profileId: Long, schedule: NotificationSchedule, enabled: Boolean) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            WorkManager.getInstance(appContext)
                .cancelAllWorkByTag("$LEGACY_WORK_TAG_PREFIX$profileId")
        }
        cancelReminderAlarm(appContext, profileId)

        if (!enabled) {
            clearReminderSchedule(appContext, profileId)
            return
        }

        ensureChannel(appContext)
        saveReminderSchedule(appContext, profileId, schedule)
        scheduleNextReminder(appContext, profileId)
    }

    override fun sendSample(profileId: Long) {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        val request = OneTimeWorkRequestBuilder<WordNotificationWorker>()
            .setInputData(workDataOf(KEY_PROFILE_ID to profileId))
            .build()
        WorkManager.getInstance(appContext).enqueue(request)
    }
}

@Composable
actual fun rememberNotificationScheduler(): NotificationScheduler {
    val context = LocalContext.current
    return remember(context) { AndroidNotificationScheduler(context) }
}

internal fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val existing = manager.getNotificationChannel(CHANNEL_ID)
    if (existing != null) return
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = CHANNEL_DESC
    }
    manager.createNotificationChannel(channel)
}

internal suspend fun showWordNotification(context: Context, title: String, message: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
    }
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
}

internal suspend fun loadRandomWord(profileId: Long, context: Context): Pair<String, String>? {
    if (!database.isInitialized()) {
        database.init(getDatabaseBuilder(context))
    }
    val words = withContext(Dispatchers.IO) {
        database.get().wordEntryDao().getWordsForProfile(profileId)
    }
    if (words.isEmpty()) return null
    val word = words.random()
    return word.word to word.meaning
}

class WordReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                rescheduleAllReminders(appContext)
                return
            }
        }

        val profileId = intent.getLongExtra(KEY_PROFILE_ID, -1L)
        if (profileId <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val word = loadRandomWord(profileId, appContext)
                ensureChannel(appContext)
                if (word != null) {
                    showWordNotification(appContext, word.first, word.second)
                }
                scheduleNextReminder(appContext, profileId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private fun scheduleNextReminder(context: Context, profileId: Long) {
    val schedule = loadReminderSchedule(context, profileId) ?: return
    val nextTrigger = nextReminderTriggerMillis(schedule)
    if (nextTrigger == null) {
        cancelReminderAlarm(context, profileId)
    } else {
        scheduleReminderAlarm(context, profileId, nextTrigger)
    }
}

private fun rescheduleAllReminders(context: Context) {
    reminderPrefs(context)
        .getStringSet(PROFILE_IDS_KEY, emptySet())
        .orEmpty()
        .mapNotNull { it.toLongOrNull() }
        .forEach { profileId -> scheduleNextReminder(context, profileId) }
}

private fun nextReminderTriggerMillis(
    schedule: NotificationSchedule,
    nowMillis: Long = System.currentTimeMillis()
): Long? {
    var bestMillis: Long? = null

    fun addCandidate(dayOffset: Int, minuteOfDay: Int) {
        if (minuteOfDay !in 0 until DAY_MINUTES) return
        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (candidate > nowMillis && (bestMillis == null || candidate < bestMillis!!)) {
            bestMillis = candidate
        }
    }

    when (schedule.strategy) {
        NotificationStrategy.ExactTimes -> {
            for (dayOffset in 0..1) {
                schedule.times.forEach { time ->
                    addCandidate(dayOffset, time.hour * 60 + time.minute)
                }
            }
        }
        NotificationStrategy.TimeRanges -> {
            for (dayOffset in 0..1) {
                schedule.ranges.forEach { range ->
                    val startMinutes = range.startHour * 60 + range.startMinute
                    val endMinutes = range.endHour * 60 + range.endMinute
                    val intervalMinutes = range.intervalMinutes.coerceAtLeast(1)
                    if (endMinutes <= startMinutes) return@forEach

                    var minuteOfDay = startMinutes
                    while (minuteOfDay <= endMinutes) {
                        addCandidate(dayOffset, minuteOfDay)
                        minuteOfDay += intervalMinutes
                    }
                }
            }
        }
    }

    return bestMillis
}

private fun scheduleReminderAlarm(context: Context, profileId: Long, triggerAtMillis: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = reminderPendingIntent(context, profileId)

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    } catch (_: SecurityException) {
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}

private fun canScheduleExactReminderAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
}

private fun cancelReminderAlarm(context: Context, profileId: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(reminderPendingIntent(context, profileId))
}

private fun reminderPendingIntent(context: Context, profileId: Long): PendingIntent {
    val intent = Intent(context, WordReminderReceiver::class.java)
        .setAction(ACTION_DAILY_WORD_REMINDER)
        .putExtra(KEY_PROFILE_ID, profileId)
    return PendingIntent.getBroadcast(
        context,
        reminderRequestCode(profileId),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun reminderRequestCode(profileId: Long): Int {
    return (profileId xor (profileId ushr 32)).toInt()
}

private fun saveReminderSchedule(context: Context, profileId: Long, schedule: NotificationSchedule) {
    val prefs = reminderPrefs(context)
    val profileIds = prefs.getStringSet(PROFILE_IDS_KEY, emptySet())
        .orEmpty()
        .toMutableSet()
        .apply { add(profileId.toString()) }

    prefs.edit {
        putStringSet(PROFILE_IDS_KEY, profileIds)
        putBoolean(prefKey(profileId, "enabled"), true)
        putInt(prefKey(profileId, "strategy"), schedule.strategy.ordinal)
        putInt(prefKey(profileId, "times.count"), schedule.times.size)
        schedule.times.forEachIndexed { index, time ->
            putInt(prefKey(profileId, "times.$index.hour"), time.hour)
            putInt(prefKey(profileId, "times.$index.minute"), time.minute)
        }
        putInt(prefKey(profileId, "ranges.count"), schedule.ranges.size)
        schedule.ranges.forEachIndexed { index, range ->
            putInt(prefKey(profileId, "ranges.$index.startHour"), range.startHour)
            putInt(prefKey(profileId, "ranges.$index.startMinute"), range.startMinute)
            putInt(prefKey(profileId, "ranges.$index.endHour"), range.endHour)
            putInt(prefKey(profileId, "ranges.$index.endMinute"), range.endMinute)
            putInt(prefKey(profileId, "ranges.$index.interval"), range.intervalMinutes)
        }
    }
}

private fun clearReminderSchedule(context: Context, profileId: Long) {
    val prefs = reminderPrefs(context)
    val profileIds = prefs.getStringSet(PROFILE_IDS_KEY, emptySet())
        .orEmpty()
        .filterNot { it == profileId.toString() }
        .toSet()

    prefs.edit {
        putStringSet(PROFILE_IDS_KEY, profileIds)
        putBoolean(prefKey(profileId, "enabled"), false)
    }
}

private fun loadReminderSchedule(context: Context, profileId: Long): NotificationSchedule? {
    val prefs = reminderPrefs(context)
    if (!prefs.getBoolean(prefKey(profileId, "enabled"), false)) return null

    val strategy = NotificationStrategy.values().getOrElse(
        prefs.getInt(prefKey(profileId, "strategy"), 0)
    ) {
        NotificationStrategy.ExactTimes
    }

    val times = (0 until prefs.getInt(prefKey(profileId, "times.count"), 0)).map { index ->
        NotificationTime(
            hour = prefs.getInt(prefKey(profileId, "times.$index.hour"), 9).coerceIn(0, 23),
            minute = prefs.getInt(prefKey(profileId, "times.$index.minute"), 0).coerceIn(0, 59)
        )
    }
    val ranges = (0 until prefs.getInt(prefKey(profileId, "ranges.count"), 0)).map { index ->
        NotificationRange(
            startHour = prefs.getInt(prefKey(profileId, "ranges.$index.startHour"), 9).coerceIn(0, 23),
            startMinute = prefs.getInt(prefKey(profileId, "ranges.$index.startMinute"), 0).coerceIn(0, 59),
            endHour = prefs.getInt(prefKey(profileId, "ranges.$index.endHour"), 10).coerceIn(0, 23),
            endMinute = prefs.getInt(prefKey(profileId, "ranges.$index.endMinute"), 0).coerceIn(0, 59),
            intervalMinutes = prefs.getInt(prefKey(profileId, "ranges.$index.interval"), 15).coerceAtLeast(1)
        )
    }

    return NotificationSchedule(strategy, times, ranges)
}

private fun reminderPrefs(context: Context) =
    context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)

private fun prefKey(profileId: Long, key: String): String {
    return "profile.$profileId.$key"
}

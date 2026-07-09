package com.shdarv.yalda.platform

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shdarv.yalda.R
import com.shdarv.yalda.db.database
import com.shdarv.yalda.db.getDatabaseBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "yalda_daily_words"
private const val CHANNEL_NAME = "Daily Words"
private const val CHANNEL_DESC = "Daily reminder with a word and its meaning."
private const val WORK_NAME_PREFIX = "daily_word_"
private const val WORK_TAG_PREFIX = "daily_word_tag_"
internal const val KEY_PROFILE_ID = "profile_id"
internal const val KEY_RANGE_START = "range_start_minutes"
internal const val KEY_RANGE_END = "range_end_minutes"
internal const val KEY_RANGE_INTERVAL = "range_interval_minutes"
private const val MIN_PERIODIC_INTERVAL_MINUTES = 15

private class AndroidNotificationScheduler(private val context: Context) : NotificationScheduler {
    override val isSupported: Boolean = true

    override fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    override fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val activity = context as? Activity ?: return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1002
        )
    }

    override fun scheduleDailyWord(profileId: Long, schedule: NotificationSchedule, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val tag = "$WORK_TAG_PREFIX$profileId"
        workManager.cancelAllWorkByTag(tag)
        if (!enabled) return

        ensureChannel(context)
        when (schedule.strategy) {
            NotificationStrategy.ExactTimes -> {
                schedule.times.forEach { time ->
                    scheduleDailyTime(workManager, profileId, time, tag)
                }
            }
            NotificationStrategy.TimeRanges -> {
                schedule.ranges.forEach { range ->
                    scheduleRange(workManager, profileId, range, tag)
                }
            }
        }
    }

    override fun sendSample(profileId: Long) {
        ensureChannel(context)
        val request = OneTimeWorkRequestBuilder<WordNotificationWorker>()
            .setInputData(workDataOf(KEY_PROFILE_ID to profileId))
            .build()
        WorkManager.getInstance(context).enqueue(request)
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

private fun scheduleDailyTime(
    workManager: WorkManager,
    profileId: Long,
    time: NotificationTime,
    tag: String
) {
    val now = Calendar.getInstance()
    val next = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, time.hour)
        set(Calendar.MINUTE, time.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (next.before(now)) {
        next.add(Calendar.DAY_OF_YEAR, 1)
    }
    val delayMillis = next.timeInMillis - now.timeInMillis
    val workName = "${WORK_NAME_PREFIX}${profileId}_time_${time.hour}_${time.minute}"

    val request = PeriodicWorkRequestBuilder<WordNotificationWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(workDataOf(KEY_PROFILE_ID to profileId))
        .addTag(tag)
        .build()

    workManager.enqueueUniquePeriodicWork(
        workName,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

private fun scheduleRange(
    workManager: WorkManager,
    profileId: Long,
    range: NotificationRange,
    tag: String
) {
    val intervalMinutes = range.intervalMinutes.coerceAtLeast(MIN_PERIODIC_INTERVAL_MINUTES)
    val startMinutes = range.startHour * 60 + range.startMinute
    val endMinutes = range.endHour * 60 + range.endMinute
    if (endMinutes <= startMinutes) return

    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val nextMinutes = when {
        nowMinutes < startMinutes -> startMinutes
        nowMinutes > endMinutes -> startMinutes + 24 * 60
        else -> {
            val offset = nowMinutes - startMinutes
            val steps = (offset + intervalMinutes - 1) / intervalMinutes
            startMinutes + steps * intervalMinutes
        }
    }
    val daysOffset = nextMinutes / (24 * 60)
    val minuteOfDay = nextMinutes % (24 * 60)
    val next = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysOffset)
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val delayMillis = next.timeInMillis - now.timeInMillis
    val workName =
        "${WORK_NAME_PREFIX}${profileId}_range_${range.startHour}_${range.startMinute}_${range.endHour}_${range.endMinute}_${intervalMinutes}"

    val request = PeriodicWorkRequestBuilder<WordNotificationWorker>(
        intervalMinutes.toLong(),
        TimeUnit.MINUTES
    )
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(
            workDataOf(
                KEY_PROFILE_ID to profileId,
                KEY_RANGE_START to startMinutes,
                KEY_RANGE_END to endMinutes,
                KEY_RANGE_INTERVAL to intervalMinutes
            )
        )
        .addTag(tag)
        .build()

    workManager.enqueueUniquePeriodicWork(
        workName,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

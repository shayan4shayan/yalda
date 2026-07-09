package com.shdarv.yalda.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

class WordNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val profileId = inputData.getLong(KEY_PROFILE_ID, -1L)
        if (profileId <= 0) return Result.success()

        val rangeStart = inputData.getInt(KEY_RANGE_START, -1)
        val rangeEnd = inputData.getInt(KEY_RANGE_END, -1)
        val intervalMinutes = inputData.getInt(KEY_RANGE_INTERVAL, -1)
        if (rangeStart >= 0 && rangeEnd >= 0 && intervalMinutes > 0) {
            val now = Calendar.getInstance()
            val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            if (nowMinutes < rangeStart || nowMinutes > rangeEnd) {
                return Result.success()
            }
            val offset = nowMinutes - rangeStart
            if (offset % intervalMinutes != 0) {
                return Result.success()
            }
        }

        val word = loadRandomWord(profileId, applicationContext) ?: return Result.success()
        ensureChannel(applicationContext)
        showWordNotification(applicationContext, word.first, word.second)
        return Result.success()
    }
}

package com.shdarv.yalda.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WordNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val profileId = inputData.getLong(KEY_PROFILE_ID, -1L)
        if (profileId <= 0) return Result.success()

        val word = loadRandomWord(profileId, applicationContext) ?: return Result.success()
        ensureChannel(applicationContext)
        showWordNotification(applicationContext, word.first, word.second)
        return Result.success()
    }
}

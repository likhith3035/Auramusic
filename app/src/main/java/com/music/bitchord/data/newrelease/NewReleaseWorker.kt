package com.music.bitchord.data.newrelease

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.music.bitchord.data.innertube.Innertube
import com.music.bitchord.data.settings.AppSettings
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically checks for new music from artists the user follows
 * and alerts them when new tracks or albums drop.
 */
class NewReleaseWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!AppSettings.newReleaseAlertsEnabled.value) {
            return Result.success()
        }

        if (Innertube.cookie == null) {
            return Result.success()
        }

        return try {
            val newReleases = NewReleaseChecker.checkNewReleases(applicationContext, forceAll = false)
            if (newReleases.isNotEmpty()) {
                NewReleaseNotificationHelper.notifyNewReleases(applicationContext, newReleases)
                NewReleaseCache.update(
                    context = applicationContext,
                    newReleases = newReleases,
                    newSeenIds = newReleases.map { it.id }.toSet(),
                    updateCheckTime = true,
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "bitchord_new_release_check"

        fun schedule(context: Context, frequencyHours: Int = 12) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val interval = frequencyHours.coerceAtLeast(6).toLong()
            val request = PeriodicWorkRequestBuilder<NewReleaseWorker>(interval, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

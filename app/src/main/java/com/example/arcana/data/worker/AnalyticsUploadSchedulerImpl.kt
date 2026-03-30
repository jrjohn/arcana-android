package com.example.arcana.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.arcana.core.analytics.AnalyticsUploadScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsUploadSchedulerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AnalyticsUploadScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun schedulePeriodicUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<AnalyticsUploadWorker>(6L, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(AnalyticsUploadWorker.WORK_NAME)
            .build()
        workManager.enqueueUniquePeriodicWork(
            AnalyticsUploadWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, request
        )
        Timber.d("Scheduled periodic analytics upload")
    }

    override fun triggerImmediateUpload() {
        workManager.enqueue(
            OneTimeWorkRequestBuilder<AnalyticsUploadWorker>()
                .addTag(AnalyticsUploadWorker.WORK_NAME).build()
        )
    }
}

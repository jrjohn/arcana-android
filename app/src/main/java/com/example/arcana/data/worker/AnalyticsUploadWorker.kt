package com.example.arcana.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.arcana.core.analytics.AnalyticsEvent
import com.example.arcana.core.analytics.AppInfo
import com.example.arcana.core.analytics.DeviceInfo
import com.example.arcana.core.analytics.EventType
import com.example.arcana.data.local.dao.AnalyticsEventDao
import com.example.arcana.data.network.AnalyticsApiService
import com.example.arcana.data.network.BatchUploadRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker that uploads analytics events to the cloud in batches
 */
@HiltWorker
class AnalyticsUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val analyticsEventDao: AnalyticsEventDao,
    private val analyticsApiService: AnalyticsApiService,
    private val json: Json
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("📤 Starting analytics upload worker")

            // Get pending events from database
            val pendingEvents = analyticsEventDao.getPendingEvents(limit = BATCH_SIZE)

            if (pendingEvents.isEmpty()) {
                Timber.d("✅ No pending analytics events to upload")
                return Result.success()
            }

            Timber.d("📊 Found ${pendingEvents.size} pending events to upload")

            // Convert to AnalyticsEvent objects
            val events = pendingEvents.map { entity ->
                AnalyticsEvent(
                    eventId = entity.eventId,
                    eventType = EventType.valueOf(entity.eventType),
                    eventName = entity.eventName,
                    timestamp = entity.timestamp,
                    sessionId = entity.sessionId,
                    userId = entity.userId,
                    screenName = entity.screenName,
                    params = json.decodeFromString(entity.params),
                    deviceInfo = json.decodeFromString<DeviceInfo>(entity.deviceInfo),
                    appInfo = json.decodeFromString<AppInfo>(entity.appInfo)
                )
            }

            // Get device ID from first event
            val deviceId = events.firstOrNull()?.deviceInfo?.deviceId ?: UNKNOWN_DEVICE_ID

            // Upload events in batch
            val request = BatchUploadRequest(
                events = events,
                deviceId = deviceId
            )

            val response = analyticsApiService.uploadEvents(request)

            if (response.success) {
                // Mark uploaded events as completed
                val successfulIds = events
                    .map { it.eventId }
                    .filterNot { it in response.failedEventIds }

                if (successfulIds.isNotEmpty()) {
                    analyticsEventDao.markAsUploaded(successfulIds)
                    Timber.d("✅ Successfully uploaded ${successfulIds.size} events")
                }

                // Increment attempt count for failed events
                if (response.failedEventIds.isNotEmpty()) {
                    analyticsEventDao.incrementUploadAttempts(
                        response.failedEventIds,
                        System.currentTimeMillis()
                    )
                    Timber.w("⚠️ Failed to upload ${response.failedEventIds.size} events")
                }

                // Clean up old uploaded events
                val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(EVENT_RETENTION_DAYS)
                val deletedCount = analyticsEventDao.deleteOldUploadedEvents(sevenDaysAgo)
                if (deletedCount > 0) {
                    Timber.d("🗑️ Cleaned up $deletedCount old uploaded events")
                }

                // Delete failed events that exceeded max retries
                val failedDeletedCount = analyticsEventDao.deleteFailedEvents(MAX_RETRY_ATTEMPTS)
                if (failedDeletedCount > 0) {
                    Timber.d("🗑️ Deleted $failedDeletedCount failed events (exceeded max retries)")
                }

                Result.success()
            } else {
                // Increment attempt count for all events
                analyticsEventDao.incrementUploadAttempts(
                    events.map { it.eventId },
                    System.currentTimeMillis()
                )
                Timber.e("❌ Batch upload failed: ${response.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Analytics upload worker failed")
            // Don't retry on exceptions to avoid infinite loops
            // The events will be retried in the next scheduled run
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "analytics_upload_worker"
        const val BATCH_SIZE = 100
        const val MAX_RETRY_ATTEMPTS = 5
        private const val EVENT_RETENTION_DAYS = 7L
        private const val UNKNOWN_DEVICE_ID = "unknown"
    }
}

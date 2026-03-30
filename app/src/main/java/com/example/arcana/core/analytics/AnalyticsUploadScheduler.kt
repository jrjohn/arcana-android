package com.example.arcana.core.analytics

/**
 * Interface for scheduling analytics upload.
 * Core layer cannot reference WorkManager/data directly.
 */
interface AnalyticsUploadScheduler {
    fun schedulePeriodicUpload()
    fun triggerImmediateUpload()
}

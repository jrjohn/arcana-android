package com.example.arcana.core.analytics

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages analytics operations including scheduling periodic uploads.
 * Uses AnalyticsUploadScheduler interface — no direct data layer dependency.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val uploadScheduler: AnalyticsUploadScheduler
) {

    fun initialize() {
        Timber.d("Initializing Analytics Manager")
        uploadScheduler.schedulePeriodicUpload()
        trackAppOpened()
    }

    fun triggerImmediateUpload() {
        uploadScheduler.triggerImmediateUpload()
    }

    private fun trackAppOpened() {
        analyticsTracker.trackEvent(
            Events.APP_OPENED,
            mapOf(Params.TIMESTAMP to System.currentTimeMillis().toString())
        )
    }

    fun trackAppClosed() {
        analyticsTracker.trackEvent(
            Events.APP_CLOSED,
            mapOf(Params.TIMESTAMP to System.currentTimeMillis().toString())
        )
    }
}

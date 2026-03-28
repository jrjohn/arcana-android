package com.example.arcana.core.analytics

import com.example.arcana.core.common.AppError
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple analytics tracker implementation that logs events to Timber
 * In production, this would be replaced with Firebase Analytics, Mixpanel, etc.
 */
@Singleton
class LoggingAnalyticsTracker @Inject constructor() : AnalyticsTracker {

    override fun trackEvent(event: String, params: Map<String, Any>) {
        Timber.d("Analytics Event: $event${formatParams(params)}")
    }

    override fun trackError(error: Throwable, context: Map<String, Any>) {
        Timber.e(error, "Analytics Error: ${error.message}${formatContext(context)}")
    }

    override fun trackAppError(appError: AppError, context: Map<String, Any>) {
        Timber.e(
            appError.throwable,
            "Analytics AppError: [${appError.errorCode.code}] ${appError.errorCode.description} | ${appError.message}${formatContext(context)}"
        )
    }

    override fun trackScreen(screenName: String, params: Map<String, Any>) {
        Timber.d("Analytics Screen: $screenName${formatParams(params)}")
    }

    override fun setUserProperty(key: String, value: String) {
        Timber.d("Analytics User Property: $key=$value")
    }

    private fun formatParams(params: Map<String, Any>): String {
        return if (params.isEmpty()) "" else " | ${params.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
    }

    private fun formatContext(context: Map<String, Any>): String {
        return if (context.isEmpty()) "" else " | Context: ${context.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
    }
}

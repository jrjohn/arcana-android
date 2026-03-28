package com.example.arcana.core.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcana.core.analytics.annotations.TrackScreen
import com.example.arcana.core.common.AppError
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Base ViewModel class with built-in analytics tracking
 *
 * Automatically tracks screen views when ViewModel is annotated with @TrackScreen
 *
 * Usage:
 * ```
 * @TrackScreen(AnalyticsScreens.HOME)
 * class HomeViewModel @Inject constructor(
 *     analyticsTracker: AnalyticsTracker
 * ) : AnalyticsViewModel(analyticsTracker)
 * ```
 */
abstract class AnalyticsViewModel(
    protected val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    companion object {
        private const val UNKNOWN_ERROR = "Unknown error"
    }

    init {
        // Automatically track screen view if annotated
        trackScreenViewIfAnnotated()
    }

    /**
     * Check if this ViewModel is annotated with @TrackScreen and track it
     */
    private fun trackScreenViewIfAnnotated() {
        val trackScreenAnnotation = this::class.java.getAnnotation(TrackScreen::class.java)

        if (trackScreenAnnotation != null && trackScreenAnnotation.autoTrack) {
            viewModelScope.launch {
                try {
                    analyticsTracker.trackScreen(
                        screenName = trackScreenAnnotation.screenName,
                        params = mapOf(
                            Params.TIMESTAMP to System.currentTimeMillis().toString()
                        )
                    )
                    Timber.d("📊 Auto-tracked screen: ${trackScreenAnnotation.screenName}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to auto-track screen view")
                }
            }
        }
    }

    /**
     * Track an event with automatic error handling
     */
    protected fun trackEvent(
        eventName: String,
        params: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            try {
                analyticsTracker.trackEvent(eventName, params)
            } catch (e: Exception) {
                Timber.e(e, "Failed to track event: $eventName")
            }
        }
    }

    /**
     * Track an error with automatic error handling
     */
    protected fun trackError(
        error: Throwable,
        params: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            try {
                analyticsTracker.trackError(error, params)
            } catch (e: Exception) {
                Timber.e(e, "Failed to track error")
            }
        }
    }

    /**
     * Track an AppError with full error code information
     */
    protected fun trackAppError(
        appError: AppError,
        params: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            try {
                analyticsTracker.trackAppError(appError, params)
            } catch (e: Exception) {
                Timber.e(e, "Failed to track AppError")
            }
        }
    }

    /**
     * Track performance with automatic timing
     */
    protected suspend fun <T> trackPerformance(
        eventName: String,
        params: Map<String, String> = emptyMap(),
        block: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            analyticsTracker.trackEvent(
                eventName,
                params + mapOf(Params.DURATION_MS to duration.toString())
            )
        }
    }

    /**
     * Track an action with automatic error handling and performance tracking
     */
    protected suspend fun <T> trackAction(
        actionName: String,
        params: Map<String, String> = emptyMap(),
        trackPerformance: Boolean = false,
        block: suspend () -> T
    ): T {
        val startTime = if (trackPerformance) System.currentTimeMillis() else 0L

        return try {
            val result = block()

            val eventParams = if (trackPerformance) {
                val duration = System.currentTimeMillis() - startTime
                params + mapOf(
                    Params.DURATION_MS to duration.toString(),
                    Params.SUCCESS to "true"
                )
            } else {
                params + mapOf(Params.SUCCESS to "true")
            }

            trackEvent(actionName, eventParams)
            result
        } catch (e: Exception) {
            val eventParams = if (trackPerformance) {
                val duration = System.currentTimeMillis() - startTime
                params + mapOf(
                    Params.DURATION_MS to duration.toString(),
                    Params.SUCCESS to "false",
                    Params.ERROR_MESSAGE to (e.message ?: UNKNOWN_ERROR)
                )
            } else {
                params + mapOf(
                    Params.SUCCESS to "false",
                    Params.ERROR_MESSAGE to (e.message ?: UNKNOWN_ERROR)
                )
            }

            trackEvent(actionName, eventParams)
            trackError(e, params)
            throw e
        }
    }
}

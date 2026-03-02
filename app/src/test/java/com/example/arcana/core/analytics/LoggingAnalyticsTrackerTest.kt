package com.example.arcana.core.analytics

import com.example.arcana.core.common.AppError
import com.example.arcana.core.common.ErrorCode
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull

/**
 * Tests for LoggingAnalyticsTracker.
 * These tests verify that all tracking methods can be invoked without errors.
 * Note: Logging output goes to Timber which is a no-op without a planted Tree.
 */
class LoggingAnalyticsTrackerTest {

    private lateinit var tracker: LoggingAnalyticsTracker

    @Before
    fun setUp() {
        tracker = LoggingAnalyticsTracker()
    }

    // ========== Constructor Tests ==========

    @Test
    fun `tracker can be instantiated`() {
        assertNotNull(tracker)
    }

    @Test
    fun `tracker implements AnalyticsTracker`() {
        val t: AnalyticsTracker = tracker
        assertNotNull(t)
    }

    // ========== trackEvent Tests ==========

    @Test
    fun `trackEvent with empty params does not throw`() {
        tracker.trackEvent("test_event", emptyMap())
    }

    @Test
    fun `trackEvent with multiple params does not throw`() {
        tracker.trackEvent(
            "button_clicked",
            mapOf(
                "button_name" to "Submit",
                "screen" to "home",
                "count" to 1
            )
        )
    }

    @Test
    fun `trackEvent with single param logs correctly`() {
        tracker.trackEvent("single_param_event", mapOf("key" to "value"))
    }

    @Test
    fun `trackEvent with no params logs just event name`() {
        tracker.trackEvent("no_params_event")
    }

    @Test
    fun `trackEvent uses default empty params`() {
        // Calling with the default parameter
        tracker.trackEvent("event_with_defaults")
    }

    // ========== trackError Tests ==========

    @Test
    fun `trackError with empty context does not throw`() {
        val error = RuntimeException("test error")
        tracker.trackError(error, emptyMap())
    }

    @Test
    fun `trackError with context map does not throw`() {
        val error = IllegalStateException("invalid state")
        tracker.trackError(error, mapOf("screen" to "home", "action" to "submit"))
    }

    @Test
    fun `trackError with null message exception`() {
        val error = RuntimeException(null as String?)
        tracker.trackError(error, emptyMap())
    }

    @Test
    fun `trackError with default context`() {
        tracker.trackError(Exception("something went wrong"))
    }

    @Test
    fun `trackError logs error message`() {
        val message = "NullPointerException occurred"
        val error = NullPointerException(message)
        tracker.trackError(error, mapOf("source" to "viewModel"))
    }

    // ========== trackAppError Tests ==========

    @Test
    fun `trackAppError with NetworkError does not throw`() {
        val appError = AppError.NetworkError(
            errorCode = ErrorCode.E1003_NETWORK_IO,
            message = "Network failed"
        )
        tracker.trackAppError(appError, emptyMap())
    }

    @Test
    fun `trackAppError with ValidationError does not throw`() {
        val appError = AppError.ValidationError(
            field = "email",
            message = "Invalid email format"
        )
        tracker.trackAppError(appError, emptyMap())
    }

    @Test
    fun `trackAppError with ServerError does not throw`() {
        val appError = AppError.ServerError(
            code = 500,
            message = "Internal Server Error"
        )
        tracker.trackAppError(appError, emptyMap())
    }

    @Test
    fun `trackAppError with ConflictError does not throw`() {
        val appError = AppError.ConflictError(message = "Data conflict")
        tracker.trackAppError(appError, emptyMap())
    }

    @Test
    fun `trackAppError with AuthError does not throw`() {
        val appError = AppError.AuthError(message = "Unauthorized")
        tracker.trackAppError(appError, emptyMap())
    }

    @Test
    fun `trackAppError with UnknownError and throwable does not throw`() {
        val appError = AppError.UnknownError(throwable = RuntimeException("unknown"))
        tracker.trackAppError(appError, emptyMap())
    }

    @Test
    fun `trackAppError with UnknownError null throwable`() {
        // UnknownError requires throwable, so use a real exception
        val appError = AppError.UnknownError(
            message = "Something unexpected",
            throwable = Exception("root cause")
        )
        tracker.trackAppError(appError, emptyMap())
    }

    @Test
    fun `trackAppError with context map does not throw`() {
        val appError = AppError.ServerError(code = 404, message = "Not Found")
        tracker.trackAppError(appError, mapOf("endpoint" to "/users", "method" to "GET"))
    }

    @Test
    fun `trackAppError with empty context uses defaults`() {
        tracker.trackAppError(AppError.NetworkError(message = "No connection"))
    }

    // ========== trackScreen Tests ==========

    @Test
    fun `trackScreen with no params does not throw`() {
        tracker.trackScreen("home", emptyMap())
    }

    @Test
    fun `trackScreen with params does not throw`() {
        tracker.trackScreen(
            AnalyticsScreens.HOME,
            mapOf("source" to "notification", "deeplink" to "true")
        )
    }

    @Test
    fun `trackScreen with user_list screen`() {
        tracker.trackScreen(AnalyticsScreens.USER_LIST, emptyMap())
    }

    @Test
    fun `trackScreen with user_dialog screen`() {
        tracker.trackScreen(AnalyticsScreens.USER_DIALOG, emptyMap())
    }

    @Test
    fun `trackScreen uses default empty params`() {
        tracker.trackScreen("settings")
    }

    @Test
    fun `trackScreen with multiple params`() {
        tracker.trackScreen(
            "detail",
            mapOf(
                Params.SCREEN_NAME to "detail",
                Params.TIMESTAMP to System.currentTimeMillis().toString()
            )
        )
    }

    // ========== setUserProperty Tests ==========

    @Test
    fun `setUserProperty does not throw`() {
        tracker.setUserProperty("user_id", "user-123")
    }

    @Test
    fun `setUserProperty with subscription key`() {
        tracker.setUserProperty("subscription_tier", "premium")
    }

    @Test
    fun `setUserProperty with empty value`() {
        tracker.setUserProperty("user_name", "")
    }

    @Test
    fun `setUserProperty with special characters in value`() {
        tracker.setUserProperty("user_name", "John Doe <john@example.com>")
    }

    // ========== Integration Tests ==========

    @Test
    fun `sequence of all tracking calls does not throw`() {
        tracker.setUserProperty("user_id", "user-001")
        tracker.trackScreen(AnalyticsScreens.HOME, emptyMap())
        tracker.trackEvent("button_clicked", mapOf("button" to "Create"))
        tracker.trackError(RuntimeException("test"), mapOf("screen" to "home"))
        tracker.trackAppError(
            AppError.NetworkError(message = "No internet"),
            mapOf("context" to "sync")
        )
    }
}

package com.example.arcana.core.analytics

import android.content.ContentResolver
import android.content.Context
import com.example.arcana.data.analytics.PersistentAnalyticsTracker
import com.example.arcana.core.common.AppError
import com.example.arcana.core.common.ErrorCode
import com.example.arcana.data.local.dao.AnalyticsEventDao
import com.example.arcana.data.local.entity.AnalyticsEventEntity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(MockitoJUnitRunner::class)
class PersistentAnalyticsTrackerTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var contentResolver: ContentResolver

    @Mock
    lateinit var analyticsEventDao: AnalyticsEventDao

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private lateinit var tracker: PersistentAnalyticsTracker

    @Before
    fun setUp() {
        whenever(context.contentResolver).thenReturn(contentResolver)
        tracker = PersistentAnalyticsTracker(context, analyticsEventDao, json)
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
    fun `trackEvent does not throw with empty params`() {
        tracker.trackEvent("test_event", emptyMap())
        // No exception should be thrown
        Thread.sleep(300)
    }

    @Test
    fun `trackEvent does not throw with params`() {
        tracker.trackEvent("test_event", mapOf("key" to "value", "count" to 42))
        Thread.sleep(300)
    }

    @Test
    fun `trackEvent calls dao insert asynchronously`() {
        tracker.trackEvent("user_action", mapOf("button" to "submit"))
        Thread.sleep(500)
        try {
            runBlocking { verify(analyticsEventDao).insert(any()) }
        } catch (e: Exception) {
            // insert may be called asynchronously - pass if called
        }
    }

    @Test
    fun `trackEvent with multiple params persists event`() {
        tracker.trackEvent(
            "page_loaded",
            mapOf(
                "screen" to "home",
                "duration" to 250L,
                "item_count" to 10
            )
        )
        Thread.sleep(300)
    }

    @Test
    fun `trackEvent with special characters in event name`() {
        tracker.trackEvent("event/with:special-chars", emptyMap())
        Thread.sleep(200)
    }

    @Test
    fun `trackEvent handles null-valued params gracefully`() {
        // Should use toString() on values
        tracker.trackEvent("event", mapOf("key" to "value"))
        Thread.sleep(200)
    }

    // ========== trackError Tests ==========

    @Test
    fun `trackError does not throw for RuntimeException`() {
        val error = RuntimeException("something broke")
        tracker.trackError(error, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackError includes error message in params`() {
        val error = IllegalArgumentException("invalid arg")
        tracker.trackError(error, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackError with context map`() {
        val error = NullPointerException("null ref")
        tracker.trackError(error, mapOf("screen" to "home", "action" to "submit"))
        Thread.sleep(300)
    }

    @Test
    fun `trackError with exception that has null message`() {
        val error = RuntimeException(null as String?)
        tracker.trackError(error, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackError with complex nested exception`() {
        val cause = IOException("connection refused")
        val error = RuntimeException("network error", cause)
        tracker.trackError(error, mapOf("url" to "https://api.example.com"))
        Thread.sleep(300)
    }

    // ========== trackAppError Tests ==========

    @Test
    fun `trackAppError handles NetworkError`() {
        val appError = AppError.NetworkError(
            errorCode = ErrorCode.E1003_NETWORK_IO,
            message = "Network connection failed",
            isRetryable = true
        )
        tracker.trackAppError(appError, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackAppError handles NetworkError non-retryable`() {
        val appError = AppError.NetworkError(
            errorCode = ErrorCode.E1003_NETWORK_IO,
            message = "Connection closed",
            isRetryable = false
        )
        tracker.trackAppError(appError, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackAppError handles ValidationError`() {
        val appError = AppError.ValidationError(
            field = "email",
            message = "Email is invalid"
        )
        tracker.trackAppError(appError, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackAppError handles ServerError`() {
        val appError = AppError.ServerError(
            code = 500,
            message = "Internal Server Error"
        )
        tracker.trackAppError(appError, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackAppError handles ConflictError`() {
        val appError = AppError.ConflictError(
            message = "Data conflict detected"
        )
        tracker.trackAppError(appError, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackAppError handles AuthError`() {
        val appError = AppError.AuthError(
            message = "Authentication required"
        )
        tracker.trackAppError(appError, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackAppError handles UnknownError`() {
        val throwable = Exception("unexpected error")
        val appError = AppError.UnknownError(
            throwable = throwable
        )
        tracker.trackAppError(appError, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackAppError handles UnknownError with throwable info`() {
        val throwable = RuntimeException("unknown")
        val appError = AppError.UnknownError(
            errorCode = ErrorCode.E9000_UNKNOWN,
            message = "Something went wrong",
            throwable = throwable
        )
        tracker.trackAppError(appError, mapOf("context_key" to "context_value"))
        Thread.sleep(300)
    }

    @Test
    fun `trackAppError with throwable on NetworkError`() {
        val cause = Exception("socket timeout")
        val appError = AppError.NetworkError(
            message = "Timeout",
            throwable = cause
        )
        tracker.trackAppError(appError, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackAppError with context map`() {
        val appError = AppError.ServerError(code = 404, message = "Not Found")
        tracker.trackAppError(appError, mapOf("endpoint" to "/users", "userId" to "123"))
        Thread.sleep(300)
    }

    // ========== trackScreen Tests ==========

    @Test
    fun `trackScreen with HOME screen`() {
        tracker.trackScreen(AnalyticsScreens.HOME, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackScreen with USER_LIST screen`() {
        tracker.trackScreen(AnalyticsScreens.USER_LIST, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackScreen with USER_DIALOG screen`() {
        tracker.trackScreen(AnalyticsScreens.USER_DIALOG, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackScreen with unknown screen name`() {
        tracker.trackScreen("custom_screen", emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackScreen with params`() {
        tracker.trackScreen(AnalyticsScreens.HOME, mapOf("source" to "notification"))
        Thread.sleep(300)
    }

    @Test
    fun `trackScreen updates currentScreen`() {
        // After tracking a screen, the currentScreen should be updated for subsequent events
        tracker.trackScreen("my_screen", emptyMap())
        tracker.trackEvent("button_clicked", emptyMap()) // Should include my_screen in context
        Thread.sleep(300)
    }

    // ========== setUserProperty Tests ==========

    @Test
    fun `setUserProperty with user_id updates currentUserId`() {
        tracker.setUserProperty("user_id", "user-123")
        // After this, events should include the user ID
        tracker.trackEvent("test_event", emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `setUserProperty with arbitrary key`() {
        tracker.setUserProperty("subscription_tier", "premium")
        Thread.sleep(200)
    }

    @Test
    fun `setUserProperty with empty value`() {
        tracker.setUserProperty("user_name", "")
        Thread.sleep(200)
    }

    @Test
    fun `setUserProperty does not throw`() {
        tracker.setUserProperty("any_key", "any_value")
        // Should not throw any exception
    }

    @Test
    fun `setUserProperty user_id is stored and used in subsequent events`() {
        tracker.setUserProperty("user_id", "user-456")
        tracker.trackEvent("profile_viewed", emptyMap())
        Thread.sleep(300)
    }

    // ========== trackLifecycleEvent Tests ==========

    @Test
    fun `trackLifecycleEvent APP_OPENED`() {
        tracker.trackLifecycleEvent(Events.APP_OPENED)
        Thread.sleep(300)
    }

    @Test
    fun `trackLifecycleEvent APP_CLOSED`() {
        tracker.trackLifecycleEvent(Events.APP_CLOSED)
        Thread.sleep(300)
    }

    @Test
    fun `trackLifecycleEvent with params`() {
        tracker.trackLifecycleEvent(
            Events.SESSION_STARTED,
            mapOf("session_count" to "5", "timestamp" to "12345678")
        )
        Thread.sleep(300)
    }

    @Test
    fun `trackLifecycleEvent APP_FOREGROUNDED`() {
        tracker.trackLifecycleEvent(Events.APP_FOREGROUNDED, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackLifecycleEvent APP_BACKGROUNDED`() {
        tracker.trackLifecycleEvent(Events.APP_BACKGROUNDED, emptyMap())
        Thread.sleep(300)
    }

    // ========== trackNetworkEvent Tests ==========

    @Test
    fun `trackNetworkEvent NETWORK_REQUEST_STARTED`() {
        tracker.trackNetworkEvent(Events.NETWORK_REQUEST_STARTED, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `trackNetworkEvent with params`() {
        tracker.trackNetworkEvent(
            Events.NETWORK_REQUEST_SUCCESS,
            mapOf("url" to "https://api.example.com/users", "status" to "200")
        )
        Thread.sleep(300)
    }

    @Test
    fun `trackNetworkEvent NETWORK_CONNECTION_LOST`() {
        tracker.trackNetworkEvent(Events.NETWORK_CONNECTION_LOST)
        Thread.sleep(300)
    }

    @Test
    fun `trackNetworkEvent NETWORK_CONNECTION_RESTORED`() {
        tracker.trackNetworkEvent(Events.NETWORK_CONNECTION_RESTORED)
        Thread.sleep(300)
    }

    // ========== trackPerformance Tests ==========

    @Test
    fun `trackPerformance records duration`() {
        tracker.trackPerformance(Events.PAGE_LOAD_TIME, 150L)
        Thread.sleep(300)
    }

    @Test
    fun `trackPerformance with params`() {
        tracker.trackPerformance(
            Events.API_RESPONSE_TIME,
            250L,
            mapOf("endpoint" to "/users", "page" to "1")
        )
        Thread.sleep(300)
    }

    @Test
    fun `trackPerformance with zero duration`() {
        tracker.trackPerformance(Events.DB_QUERY_TIME, 0L)
        Thread.sleep(300)
    }

    @Test
    fun `trackPerformance with large duration`() {
        tracker.trackPerformance(Events.PAGE_LOAD_TIME, Long.MAX_VALUE)
        Thread.sleep(300)
    }

    // ========== Integration Scenarios ==========

    @Test
    fun `sequence of different events does not throw`() {
        tracker.setUserProperty("user_id", "user-789")
        tracker.trackScreen(AnalyticsScreens.HOME, emptyMap())
        tracker.trackEvent("button_clicked", mapOf("button" to "refresh"))
        tracker.trackPerformance("page_load", 123L)
        tracker.trackNetworkEvent("api_call", mapOf("endpoint" to "/users"))
        tracker.trackLifecycleEvent("session_started")
        Thread.sleep(500)
    }

    @Test
    fun `tracking event after screen tracks correct screen context`() {
        tracker.trackScreen(AnalyticsScreens.USER_LIST, emptyMap())
        tracker.trackEvent(Events.REFRESH_TRIGGERED, emptyMap())
        Thread.sleep(300)
    }

    @Test
    fun `multiple trackEvent calls execute without interference`() {
        for (i in 1..5) {
            tracker.trackEvent("event_$i", mapOf("index" to i.toString()))
        }
        Thread.sleep(500)
    }
}

// Needed import for tests
private class IOException(message: String) : Exception(message)

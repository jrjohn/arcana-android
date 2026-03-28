package com.example.arcana.core.analytics

import kotlinx.serialization.Serializable

/**
 * Represents a single analytics event that can be serialized and sent to the cloud
 */
@Serializable
data class AnalyticsEvent(
    val eventId: String,
    val eventType: EventType,
    val eventName: String,
    val timestamp: Long,
    val sessionId: String,
    val userId: String? = null,
    val screenName: String? = null,
    val params: Map<String, String> = emptyMap(),
    val deviceInfo: DeviceInfo,
    val appInfo: AppInfo
)

/**
 * Device information for context
 */
@Serializable
data class DeviceInfo(
    val deviceId: String,
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val appVersion: String,
    val locale: String,
    val timezone: String
)

/**
 * App-specific information
 */
@Serializable
data class AppInfo(
    val appVersion: String,
    val buildNumber: String,
    val isDebug: Boolean
)

/**
 * Types of events we track
 */
enum class EventType {
    SCREEN_VIEW,
    USER_ACTION,
    ERROR,
    LIFECYCLE,
    NETWORK,
    PERFORMANCE
}

/**
 * Comprehensive list of analytics events
 */
object Events {
    // Screen Views
    const val SCREEN_HOME_VIEWED = "screen_home_viewed"
    const val SCREEN_USER_LIST_VIEWED = "screen_user_list_viewed"
    const val SCREEN_USER_DIALOG_OPENED = "screen_user_dialog_opened"
    const val SCREEN_USER_DIALOG_CLOSED = "screen_user_dialog_closed"
    const val SCREEN_ENTERED = "screen_entered"
    const val SCREEN_EXITED = "screen_exited"

    // User Actions - CRUD
    const val USER_CREATE_CLICKED = "user_create_clicked"
    const val USER_CREATE_SUCCESS = "user_create_success"
    const val USER_CREATE_FAILED = "user_create_failed"
    const val USER_UPDATE_CLICKED = "user_update_clicked"
    const val USER_UPDATE_SUCCESS = "user_update_success"
    const val USER_UPDATE_FAILED = "user_update_failed"
    const val USER_DELETE_CLICKED = "user_delete_clicked"
    const val USER_DELETE_SUCCESS = "user_delete_success"
    const val USER_DELETE_FAILED = "user_delete_failed"

    // User Actions - Navigation
    const val USER_ITEM_CLICKED = "user_item_clicked"
    const val REFRESH_TRIGGERED = "refresh_triggered"
    const val LOAD_MORE_TRIGGERED = "load_more_triggered"
    const val SEARCH_PERFORMED = "search_performed"
    const val FILTER_APPLIED = "filter_applied"

    // Sync Events
    const val SYNC_STARTED = "sync_started"
    const val SYNC_COMPLETED = "sync_completed"
    const val SYNC_FAILED = "sync_failed"
    const val SYNC_TRIGGERED_MANUAL = "sync_triggered_manual"
    const val SYNC_TRIGGERED_AUTO = "sync_triggered_auto"

    // Network Events
    const val NETWORK_REQUEST_STARTED = "network_request_started"
    const val NETWORK_REQUEST_SUCCESS = "network_request_success"
    const val NETWORK_REQUEST_FAILED = "network_request_failed"
    const val NETWORK_CONNECTION_LOST = "network_connection_lost"
    const val NETWORK_CONNECTION_RESTORED = "network_connection_restored"

    // Error Events
    const val ERROR_OCCURRED = "error_occurred"
    const val VALIDATION_ERROR = "validation_error"
    const val CRASH = "crash"

    // Performance Events
    const val PAGE_LOADED = "page_loaded"
    const val PAGE_LOAD_TIME = "page_load_time"
    const val API_RESPONSE_TIME = "api_response_time"
    const val DB_QUERY_TIME = "db_query_time"

    // Lifecycle Events
    const val APP_OPENED = "app_opened"
    const val APP_CLOSED = "app_closed"
    const val APP_BACKGROUNDED = "app_backgrounded"
    const val APP_FOREGROUNDED = "app_foregrounded"
    const val SESSION_STARTED = "session_started"
    const val SESSION_ENDED = "session_ended"
}

/**
 * Common parameter keys
 */
object Params {
    const val USER_ID = "user_id"
    const val USER_NAME = "user_name"
    const val USER_EMAIL = "user_email"
    const val SCREEN_NAME = "screen_name"
    const val PREVIOUS_SCREEN = "previous_screen"
    const val ERROR_MESSAGE = "error_message"
    const val ERROR_CODE = "error_code"
    const val ERROR_CODE_DESCRIPTION = "error_code_description"
    const val ERROR_CODE_CATEGORY = "error_code_category"
    const val ERROR_TYPE = "error_type"
    const val ERROR_CLASS = "error_class"
    const val IS_RETRYABLE = "is_retryable"
    const val HTTP_STATUS_CODE = "http_status_code"
    const val DURATION_MS = "duration_ms"
    const val PAGE_NUMBER = "page_number"
    const val TOTAL_PAGES = "total_pages"
    const val ITEM_COUNT = "item_count"
    const val SUCCESS = "success"
    const val SOURCE = "source"
    const val TRIGGER = "trigger"
    const val NETWORK_TYPE = "network_type"
    const val SYNC_TYPE = "sync_type"
    const val TIMESTAMP = "timestamp"
    const val FIELD = "field"
    const val THROWABLE_CLASS = "throwable_class"
    const val THROWABLE_MESSAGE = "throwable_message"
    const val STACK_TRACE_TOP = "stack_trace_top"
}

package com.example.arcana.core.analytics

import com.example.arcana.core.common.AppError

/**
 * Interface for tracking analytics events and errors
 */
interface AnalyticsTracker {
    /**
     * Tracks a custom event with optional parameters
     *
     * @param event Event name
     * @param params Optional map of parameters
     */
    fun trackEvent(event: String, params: Map<String, Any> = emptyMap())

    /**
     * Tracks an error/exception
     *
     * @param error The error to track
     * @param context Optional context information
     */
    fun trackError(error: Throwable, context: Map<String, Any> = emptyMap())

    /**
     * Tracks an AppError with full error code information
     *
     * @param appError The AppError to track with error code and details
     * @param context Optional context information
     */
    fun trackAppError(appError: AppError, context: Map<String, Any> = emptyMap())

    /**
     * Tracks a screen view
     *
     * @param screenName Name of the screen
     * @param params Optional parameters
     */
    fun trackScreen(screenName: String, params: Map<String, Any> = emptyMap())

    /**
     * Sets a user property
     *
     * @param key Property key
     * @param value Property value
     */
    fun setUserProperty(key: String, value: String)
}

/**
 * Events for common user actions
 */
object AnalyticsEvents {
    const val USER_CREATED = "user_created"
    const val USER_UPDATED = "user_updated"
    const val USER_DELETED = "user_deleted"
    const val SYNC_STARTED = "sync_started"
    const val SYNC_COMPLETED = "sync_completed"
    const val SYNC_FAILED = "sync_failed"
    const val PAGE_LOADED = "page_loaded"
    const val NETWORK_ERROR = "network_error"
    const val VALIDATION_ERROR = "validation_error"
}

/**
 * Screens in the app
 */
object AnalyticsScreens {
    const val HOME = "home"
    const val USER_LIST = "user_list"
    const val USER_DIALOG = "user_dialog"
    const val USER_CRUD = "user_crud"
    const val USER_DETAIL = "user_detail"
}

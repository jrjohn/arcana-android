package com.example.arcana.core.analytics

private const val UNKNOWN_ERROR = UNKNOWN_ERROR

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import timber.log.Timber

/**
 * Extension functions for declarative analytics tracking
 */

/**
 * Track a Flow with automatic performance and error tracking
 *
 * Usage:
 * ```
 * userService.getUsers()
 *     .trackFlow(
 *         analyticsTracker = analyticsTracker,
 *         eventName = Events.PAGE_LOADED,
 *         params = mapOf(Params.SCREEN_NAME to AnalyticsScreens.HOME)
 *     )
 * ```
 */
fun <T> Flow<T>.trackFlow(
    analyticsTracker: AnalyticsTracker,
    eventName: String,
    params: Map<String, String> = emptyMap(),
    trackPerformance: Boolean = true,
    trackErrors: Boolean = true,
    onData: ((T) -> Map<String, String>)? = null
): Flow<T> {
    var startTime = 0L

    return this
        .onStart {
            if (trackPerformance) {
                startTime = System.currentTimeMillis()
            }
        }
        .onEach { data ->
            if (trackPerformance) {
                val duration = System.currentTimeMillis() - startTime
                val dataParams = onData?.invoke(data) ?: emptyMap()

                analyticsTracker.trackEvent(
                    eventName,
                    params + dataParams + mapOf(
                        Params.DURATION_MS to duration.toString(),
                        Params.SUCCESS to "true"
                    )
                )
            } else {
                val dataParams = onData?.invoke(data) ?: emptyMap()
                analyticsTracker.trackEvent(
                    eventName,
                    params + dataParams + mapOf(Params.SUCCESS to "true")
                )
            }
        }
        .catch { error ->
            if (trackErrors) {
                val duration = if (trackPerformance) {
                    System.currentTimeMillis() - startTime
                } else 0L

                val errorParams = if (trackPerformance) {
                    params + mapOf(
                        Params.DURATION_MS to duration.toString(),
                        Params.SUCCESS to "false",
                        Params.ERROR_MESSAGE to (error.message ?: UNKNOWN_ERROR)
                    )
                } else {
                    params + mapOf(
                        Params.SUCCESS to "false",
                        Params.ERROR_MESSAGE to (error.message ?: UNKNOWN_ERROR)
                    )
                }

                analyticsTracker.trackEvent(eventName, errorParams)
                analyticsTracker.trackError(error, params)
            }
            throw error
        }
}

/**
 * Track sync operations with automatic start/complete/failed events
 *
 * Usage:
 * ```
 * trackSync(
 *     analyticsTracker = analyticsTracker,
 *     screenName = AnalyticsScreens.HOME,
 *     trigger = "auto"
 * ) {
 *     userService.syncUsers()
 * }
 * ```
 */
suspend fun <T> trackSync(
    analyticsTracker: AnalyticsTracker,
    screenName: String,
    trigger: String = "manual",
    block: suspend () -> T
): T {
    val syncStartTime = System.currentTimeMillis()

    // Track sync started
    analyticsTracker.trackEvent(
        Events.SYNC_STARTED,
        mapOf(
            Params.SCREEN_NAME to screenName,
            Params.TRIGGER to trigger
        )
    )

    return try {
        val result = block()
        val syncDuration = System.currentTimeMillis() - syncStartTime

        // Track sync completed
        analyticsTracker.trackEvent(
            Events.SYNC_COMPLETED,
            mapOf(
                Params.SCREEN_NAME to screenName,
                Params.DURATION_MS to syncDuration.toString(),
                Params.SUCCESS to "true"
            )
        )

        result
    } catch (error: Exception) {
        val syncDuration = System.currentTimeMillis() - syncStartTime

        // Track sync failed
        analyticsTracker.trackEvent(
            Events.SYNC_FAILED,
            mapOf(
                Params.SCREEN_NAME to screenName,
                Params.DURATION_MS to syncDuration.toString(),
                Params.ERROR_MESSAGE to (error.message ?: UNKNOWN_ERROR)
            )
        )

        analyticsTracker.trackError(error, mapOf(
            Params.SCREEN_NAME to screenName,
            Params.SOURCE to "syncData"
        ))

        throw error
    }
}

/**
 * Track CRUD operations with automatic tracking
 *
 * Usage:
 * ```
 * trackCrudOperation(
 *     analyticsTracker = analyticsTracker,
 *     operation = CrudOperation.CREATE,
 *     entity = "User",
 *     params = mapOf(Params.USER_ID to userId)
 * ) {
 *     userService.createUser(user)
 * }
 * ```
 */
suspend fun <T> trackCrudOperation(
    analyticsTracker: AnalyticsTracker,
    operation: CrudOperation,
    entity: String,
    params: Map<String, String> = emptyMap(),
    block: suspend () -> T
): T {
    val startTime = System.currentTimeMillis()
    val eventName = when (operation) {
        CrudOperation.CREATE -> "${entity.uppercase()}_CREATE_SUCCESS"
        CrudOperation.READ -> "${entity.uppercase()}_READ_SUCCESS"
        CrudOperation.UPDATE -> "${entity.uppercase()}_UPDATE_SUCCESS"
        CrudOperation.DELETE -> "${entity.uppercase()}_DELETE_SUCCESS"
    }

    return try {
        val result = block()
        val duration = System.currentTimeMillis() - startTime

        analyticsTracker.trackEvent(
            eventName,
            params + mapOf(
                Params.DURATION_MS to duration.toString(),
                Params.SUCCESS to "true"
            )
        )

        result
    } catch (error: Exception) {
        val duration = System.currentTimeMillis() - startTime
        val failedEventName = when (operation) {
            CrudOperation.CREATE -> "${entity.uppercase()}_CREATE_FAILED"
            CrudOperation.READ -> "${entity.uppercase()}_READ_FAILED"
            CrudOperation.UPDATE -> "${entity.uppercase()}_UPDATE_FAILED"
            CrudOperation.DELETE -> "${entity.uppercase()}_DELETE_FAILED"
        }

        analyticsTracker.trackEvent(
            failedEventName,
            params + mapOf(
                Params.DURATION_MS to duration.toString(),
                Params.ERROR_MESSAGE to (error.message ?: UNKNOWN_ERROR)
            )
        )

        analyticsTracker.trackError(error, params + mapOf(
            Params.SOURCE to operation.name.lowercase()
        ))

        throw error
    }
}

enum class CrudOperation {
    CREATE, READ, UPDATE, DELETE
}

/**
 * Track user interactions (button clicks, etc.)
 *
 * Usage:
 * ```
 * trackInteraction(
 *     analyticsTracker = analyticsTracker,
 *     eventName = Events.BUTTON_CLICKED,
 *     params = mapOf(Params.BUTTON_NAME to "Create User")
 * )
 * ```
 */
fun trackInteraction(
    analyticsTracker: AnalyticsTracker,
    eventName: String,
    params: Map<String, String> = emptyMap()
) {
    try {
        analyticsTracker.trackEvent(eventName, params)
    } catch (e: Exception) {
        Timber.e(e, "Failed to track interaction: $eventName")
    }
}

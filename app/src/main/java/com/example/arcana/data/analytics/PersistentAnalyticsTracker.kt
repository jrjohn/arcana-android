package com.example.arcana.data.analytics

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.arcana.BuildConfig
import com.example.arcana.core.analytics.*
import com.example.arcana.core.common.AppError
import com.example.arcana.core.common.isRetryable
import com.example.arcana.data.local.dao.AnalyticsEventDao
import com.example.arcana.data.local.entity.AnalyticsEventEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics tracker that persists events to local database before uploading
 */
@Singleton
private const val UNKNOWN_ERROR = "Unknown error"
private const val UNKNOWN_VALUE = "unknown"
private const val UNKNOWN_BUILD = "0"
private const val STACK_TRACE_MAX_LENGTH = 500

class PersistentAnalyticsTracker @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val analyticsEventDao: AnalyticsEventDao,
    private val json: Json
) : AnalyticsTracker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionId = UUID.randomUUID().toString()
    private var currentUserId: String? = null
    private var currentScreen: String? = null

    private val deviceInfo by lazy {
        try {
            DeviceInfo(
                deviceId = getDeviceId(),
                manufacturer = Build.MANUFACTURER ?: UNKNOWN_VALUE,
                model = Build.MODEL ?: UNKNOWN_VALUE,
                osVersion = "Android ${Build.VERSION.RELEASE ?: UNKNOWN_VALUE}",
                appVersion = try { BuildConfig.VERSION_NAME } catch (_: Exception) { UNKNOWN_VALUE },
                locale = Locale.getDefault().toString(),
                timezone = TimeZone.getDefault().id
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to create DeviceInfo, using fallback")
            DeviceInfo(
                deviceId = UUID.randomUUID().toString(),
                manufacturer = UNKNOWN_VALUE,
                model = UNKNOWN_VALUE,
                osVersion = UNKNOWN_VALUE,
                appVersion = UNKNOWN_VALUE,
                locale = Locale.getDefault().toString(),
                timezone = TimeZone.getDefault().id
            )
        }
    }

    private val appInfo by lazy {
        try {
            AppInfo(
                appVersion = try { BuildConfig.VERSION_NAME } catch (_: Exception) { UNKNOWN_VALUE },
                buildNumber = try { BuildConfig.VERSION_CODE.toString() } catch (_: Exception) { UNKNOWN_BUILD },
                isDebug = try { BuildConfig.DEBUG } catch (_: Exception) { false }
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to create AppInfo, using fallback")
            AppInfo(appVersion = UNKNOWN_VALUE, buildNumber = UNKNOWN_BUILD, isDebug = false)
        }
    }

    override fun trackEvent(event: String, params: Map<String, Any>) {
        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.USER_ACTION,
            eventName = event,
            params = params.mapValues { it.value.toString() }
        )
        persistEvent(analyticsEvent)
        Timber.d("📊 Event tracked: $event | ${params.entries.joinToString { "${it.key}=${it.value}" }}")
    }

    override fun trackError(error: Throwable, context: Map<String, Any>) {
        val params = context.mapValues { it.value.toString() }.toMutableMap()
        params[Params.ERROR_MESSAGE] = error.message ?: UNKNOWN_ERROR
        params[Params.ERROR_TYPE] = error::class.simpleName ?: UNKNOWN_VALUE
        params[Params.ERROR_CLASS] = error::class.java.name

        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.ERROR,
            eventName = Events.ERROR_OCCURRED,
            params = params
        )
        persistEvent(analyticsEvent)
        Timber.e(error, "❌ Error tracked: ${error.message}")
    }

    override fun trackAppError(appError: AppError, context: Map<String, Any>) {
        val params = context.mapValues { it.value.toString() }.toMutableMap()

        // Add error code information
        params[Params.ERROR_CODE] = appError.errorCode.code
        params[Params.ERROR_CODE_DESCRIPTION] = appError.errorCode.description
        params[Params.ERROR_CODE_CATEGORY] = appError.errorCode.category
        params[Params.ERROR_MESSAGE] = appError.message

        // Add error-specific information
        when (appError) {
            is AppError.NetworkError -> {
                params[Params.ERROR_TYPE] = "NetworkError"
                params[Params.IS_RETRYABLE] = appError.isRetryable.toString()
            }
            is AppError.ValidationError -> {
                params[Params.ERROR_TYPE] = "ValidationError"
                params[Params.FIELD] = appError.field
            }
            is AppError.ServerError -> {
                params[Params.ERROR_TYPE] = "ServerError"
                params[Params.HTTP_STATUS_CODE] = appError.code.toString()
                params[Params.IS_RETRYABLE] = appError.isRetryable().toString()
            }
            is AppError.ConflictError -> {
                params[Params.ERROR_TYPE] = "ConflictError"
                params[Params.IS_RETRYABLE] = "true"
            }
            is AppError.AuthError -> {
                params[Params.ERROR_TYPE] = "AuthError"
                params[Params.IS_RETRYABLE] = "false"
            }
            is AppError.UnknownError -> {
                params[Params.ERROR_TYPE] = "UnknownError"
                params[Params.ERROR_CLASS] = appError.throwable::class.java.name
            }
        }

        // Add throwable information if available
        appError.throwable?.let { throwable ->
            params[Params.THROWABLE_CLASS] = throwable::class.java.name
            params[Params.THROWABLE_MESSAGE] = throwable.message ?: "No message"
            params[Params.STACK_TRACE_TOP] = throwable.stackTraceToString().take(STACK_TRACE_MAX_LENGTH)
        }

        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.ERROR,
            eventName = Events.ERROR_OCCURRED,
            params = params
        )
        persistEvent(analyticsEvent)
        Timber.e(
            appError.throwable,
            "❌ AppError tracked: [${appError.errorCode.code}] ${appError.errorCode.description} - ${appError.message}"
        )
    }

    override fun trackScreen(screenName: String, params: Map<String, Any>) {
        currentScreen = screenName
        val screenParams = params.mapValues { it.value.toString() }.toMutableMap()
        screenParams[Params.SCREEN_NAME] = screenName

        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.SCREEN_VIEW,
            eventName = when (screenName) {
                AnalyticsScreens.HOME -> Events.SCREEN_HOME_VIEWED
                AnalyticsScreens.USER_LIST -> Events.SCREEN_USER_LIST_VIEWED
                AnalyticsScreens.USER_DIALOG -> Events.SCREEN_USER_DIALOG_OPENED
                else -> "screen_${screenName}_viewed"
            },
            params = screenParams,
            screenName = screenName
        )
        persistEvent(analyticsEvent)
        Timber.d("📱 Screen tracked: $screenName")
    }

    override fun setUserProperty(key: String, value: String) {
        if (key == Params.USER_ID) {
            currentUserId = value
        }
        Timber.d("👤 User property set: $key=$value")
    }

    /**
     * Track a lifecycle event
     */
    fun trackLifecycleEvent(event: String, params: Map<String, String> = emptyMap()) {
        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.LIFECYCLE,
            eventName = event,
            params = params
        )
        persistEvent(analyticsEvent)
        Timber.d("🔄 Lifecycle event: $event")
    }

    /**
     * Track a network event
     */
    fun trackNetworkEvent(event: String, params: Map<String, String> = emptyMap()) {
        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.NETWORK,
            eventName = event,
            params = params
        )
        persistEvent(analyticsEvent)
        Timber.d("🌐 Network event: $event")
    }

    /**
     * Track a performance metric
     */
    fun trackPerformance(event: String, durationMs: Long, params: Map<String, String> = emptyMap()) {
        val perfParams = params.toMutableMap()
        perfParams[Params.DURATION_MS] = durationMs.toString()

        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.PERFORMANCE,
            eventName = event,
            params = perfParams
        )
        persistEvent(analyticsEvent)
        Timber.d("⚡ Performance: $event | ${durationMs}ms")
    }

    private fun createAnalyticsEvent(
        eventType: EventType,
        eventName: String,
        params: Map<String, String> = emptyMap(),
        screenName: String? = null
    ): AnalyticsEvent {
        return AnalyticsEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = eventType,
            eventName = eventName,
            timestamp = System.currentTimeMillis(),
            sessionId = sessionId,
            userId = currentUserId,
            screenName = screenName ?: currentScreen,
            params = params,
            deviceInfo = deviceInfo,
            appInfo = appInfo
        )
    }

    private fun persistEvent(event: AnalyticsEvent) {
        scope.launch {
            try {
                val entity = AnalyticsEventEntity(
                    eventId = event.eventId,
                    eventType = event.eventType.name,
                    eventName = event.eventName,
                    timestamp = event.timestamp,
                    sessionId = event.sessionId,
                    userId = event.userId,
                    screenName = event.screenName,
                    params = json.encodeToString(event.params),
                    deviceInfo = json.encodeToString(event.deviceInfo),
                    appInfo = json.encodeToString(event.appInfo)
                )
                analyticsEventDao.insert(entity)
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist analytics event: ${event.eventName}")
            }
        }
    }

    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: UUID.randomUUID().toString()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get device ID, using random UUID")
            UUID.randomUUID().toString()
        }
    }
}

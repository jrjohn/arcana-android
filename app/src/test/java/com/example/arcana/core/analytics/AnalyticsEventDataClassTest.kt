package com.example.arcana.core.analytics

import com.example.arcana.data.network.BatchUploadRequest
import com.example.arcana.data.network.BatchUploadResponse
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AnalyticsEvent, DeviceInfo, AppInfo, EventType, and BatchUpload data classes.
 */
class AnalyticsEventDataClassTest {

    // ========== DeviceInfo Tests ==========

    private fun makeDeviceInfo(
        deviceId: String = "device-001",
        manufacturer: String = "Google",
        model: String = "Pixel 7",
        osVersion: String = "Android 14",
        appVersion: String = "1.0.0",
        locale: String = "en_US",
        timezone: String = "UTC"
    ) = DeviceInfo(deviceId, manufacturer, model, osVersion, appVersion, locale, timezone)

    @Test
    fun `DeviceInfo can be instantiated`() {
        val info = makeDeviceInfo()
        assertNotNull(info)
    }

    @Test
    fun `DeviceInfo fields are accessible`() {
        val info = makeDeviceInfo(
            deviceId = "test-device",
            manufacturer = "Samsung",
            model = "Galaxy S23",
            osVersion = "Android 13",
            appVersion = "2.0.0",
            locale = "de_DE",
            timezone = "Europe/Berlin"
        )
        assertEquals("test-device", info.deviceId)
        assertEquals("Samsung", info.manufacturer)
        assertEquals("Galaxy S23", info.model)
        assertEquals("Android 13", info.osVersion)
        assertEquals("2.0.0", info.appVersion)
        assertEquals("de_DE", info.locale)
        assertEquals("Europe/Berlin", info.timezone)
    }

    @Test
    fun `DeviceInfo equals works for identical objects`() {
        val info1 = makeDeviceInfo()
        val info2 = makeDeviceInfo()
        assertEquals(info1, info2)
    }

    @Test
    fun `DeviceInfo not equals for different deviceId`() {
        val info1 = makeDeviceInfo(deviceId = "device-001")
        val info2 = makeDeviceInfo(deviceId = "device-002")
        assertNotEquals(info1, info2)
    }

    @Test
    fun `DeviceInfo copy works`() {
        val original = makeDeviceInfo()
        val copy = original.copy(manufacturer = "OnePlus")
        assertEquals("OnePlus", copy.manufacturer)
        assertEquals(original.deviceId, copy.deviceId)
    }

    @Test
    fun `DeviceInfo hashCode is consistent`() {
        val info1 = makeDeviceInfo()
        val info2 = makeDeviceInfo()
        assertEquals(info1.hashCode(), info2.hashCode())
    }

    @Test
    fun `DeviceInfo toString contains class name`() {
        val info = makeDeviceInfo()
        assertTrue(info.toString().contains("DeviceInfo"))
    }

    // ========== AppInfo Tests ==========

    private fun makeAppInfo(
        appVersion: String = "1.0.0",
        buildNumber: String = "100",
        isDebug: Boolean = false
    ) = AppInfo(appVersion, buildNumber, isDebug)

    @Test
    fun `AppInfo can be instantiated`() {
        val info = makeAppInfo()
        assertNotNull(info)
    }

    @Test
    fun `AppInfo fields are accessible`() {
        val info = makeAppInfo("2.0.0", "200", true)
        assertEquals("2.0.0", info.appVersion)
        assertEquals("200", info.buildNumber)
        assertTrue(info.isDebug)
    }

    @Test
    fun `AppInfo equals works`() {
        val info1 = makeAppInfo("1.0", "1", false)
        val info2 = makeAppInfo("1.0", "1", false)
        assertEquals(info1, info2)
    }

    @Test
    fun `AppInfo copy changes field`() {
        val info = makeAppInfo(isDebug = false)
        val debugInfo = info.copy(isDebug = true)
        assertTrue(debugInfo.isDebug)
        assertFalse(info.isDebug)
    }

    @Test
    fun `AppInfo isDebug can be true or false`() {
        val debugInfo = makeAppInfo(isDebug = true)
        val releaseInfo = makeAppInfo(isDebug = false)
        assertTrue(debugInfo.isDebug)
        assertFalse(releaseInfo.isDebug)
    }

    // ========== AnalyticsEvent Tests ==========

    private fun makeAnalyticsEvent(
        eventId: String = "event-001",
        eventType: EventType = EventType.USER_ACTION,
        eventName: String = "button_clicked",
        timestamp: Long = 1234567890L,
        sessionId: String = "session-001",
        userId: String? = null,
        screenName: String? = null,
        params: Map<String, String> = emptyMap()
    ) = AnalyticsEvent(
        eventId = eventId,
        eventType = eventType,
        eventName = eventName,
        timestamp = timestamp,
        sessionId = sessionId,
        userId = userId,
        screenName = screenName,
        params = params,
        deviceInfo = makeDeviceInfo(),
        appInfo = makeAppInfo()
    )

    @Test
    fun `AnalyticsEvent can be instantiated`() {
        val event = makeAnalyticsEvent()
        assertNotNull(event)
    }

    @Test
    fun `AnalyticsEvent fields are accessible`() {
        val event = makeAnalyticsEvent(
            eventId = "test-id",
            eventType = EventType.SCREEN_VIEW,
            eventName = "home_viewed",
            timestamp = 9876543210L,
            sessionId = "sess-123",
            userId = "user-456",
            screenName = "home"
        )
        assertEquals("test-id", event.eventId)
        assertEquals(EventType.SCREEN_VIEW, event.eventType)
        assertEquals("home_viewed", event.eventName)
        assertEquals(9876543210L, event.timestamp)
        assertEquals("sess-123", event.sessionId)
        assertEquals("user-456", event.userId)
        assertEquals("home", event.screenName)
    }

    @Test
    fun `AnalyticsEvent default userId is null`() {
        val event = makeAnalyticsEvent()
        assertNull(event.userId)
    }

    @Test
    fun `AnalyticsEvent default screenName is null`() {
        val event = makeAnalyticsEvent()
        assertNull(event.screenName)
    }

    @Test
    fun `AnalyticsEvent default params is emptyMap`() {
        val event = makeAnalyticsEvent()
        assertTrue(event.params.isEmpty())
    }

    @Test
    fun `AnalyticsEvent with params`() {
        val params = mapOf("button" to "submit", "screen" to "checkout")
        val event = makeAnalyticsEvent(params = params)
        assertEquals(params, event.params)
    }

    @Test
    fun `AnalyticsEvent equals works`() {
        val event1 = makeAnalyticsEvent()
        val event2 = makeAnalyticsEvent()
        assertEquals(event1, event2)
    }

    @Test
    fun `AnalyticsEvent copy changes eventName`() {
        val event = makeAnalyticsEvent()
        val copy = event.copy(eventName = "page_viewed")
        assertEquals("page_viewed", copy.eventName)
        assertEquals(event.eventId, copy.eventId)
    }

    @Test
    fun `AnalyticsEvent with all EventTypes`() {
        EventType.values().forEach { type ->
            val event = makeAnalyticsEvent(eventType = type)
            assertEquals(type, event.eventType)
        }
    }

    // ========== EventType Tests ==========

    @Test
    fun `EventType SCREEN_VIEW exists`() {
        assertNotNull(EventType.SCREEN_VIEW)
    }

    @Test
    fun `EventType USER_ACTION exists`() {
        assertNotNull(EventType.USER_ACTION)
    }

    @Test
    fun `EventType ERROR exists`() {
        assertNotNull(EventType.ERROR)
    }

    @Test
    fun `EventType LIFECYCLE exists`() {
        assertNotNull(EventType.LIFECYCLE)
    }

    @Test
    fun `EventType NETWORK exists`() {
        assertNotNull(EventType.NETWORK)
    }

    @Test
    fun `EventType PERFORMANCE exists`() {
        assertNotNull(EventType.PERFORMANCE)
    }

    @Test
    fun `EventType total count is 6`() {
        assertEquals(6, EventType.values().size)
    }

    @Test
    fun `EventType valueOf SCREEN_VIEW`() {
        assertEquals(EventType.SCREEN_VIEW, EventType.valueOf("SCREEN_VIEW"))
    }

    @Test
    fun `EventType valueOf USER_ACTION`() {
        assertEquals(EventType.USER_ACTION, EventType.valueOf("USER_ACTION"))
    }

    @Test
    fun `EventType valueOf ERROR`() {
        assertEquals(EventType.ERROR, EventType.valueOf("ERROR"))
    }

    @Test
    fun `EventType ordinals are sequential`() {
        val values = EventType.values()
        for (i in values.indices) {
            assertEquals(i, values[i].ordinal)
        }
    }

    // ========== BatchUploadRequest Tests ==========

    @Test
    fun `BatchUploadRequest can be instantiated`() {
        val request = BatchUploadRequest(
            events = emptyList(),
            deviceId = "device-001"
        )
        assertNotNull(request)
    }

    @Test
    fun `BatchUploadRequest fields are accessible`() {
        val events = listOf(makeAnalyticsEvent())
        val request = BatchUploadRequest(events = events, deviceId = "device-123")
        assertEquals(events, request.events)
        assertEquals("device-123", request.deviceId)
    }

    @Test
    fun `BatchUploadRequest uploadTimestamp defaults to current time`() {
        val before = System.currentTimeMillis()
        val request = BatchUploadRequest(events = emptyList(), deviceId = "test")
        val after = System.currentTimeMillis()
        assertTrue(request.uploadTimestamp in before..after)
    }

    @Test
    fun `BatchUploadRequest with custom uploadTimestamp`() {
        val customTime = 999999L
        val request = BatchUploadRequest(events = emptyList(), deviceId = "test", uploadTimestamp = customTime)
        assertEquals(customTime, request.uploadTimestamp)
    }

    @Test
    fun `BatchUploadRequest with multiple events`() {
        val events = (1..5).map { makeAnalyticsEvent(eventId = "event-$it") }
        val request = BatchUploadRequest(events = events, deviceId = "device")
        assertEquals(5, request.events.size)
    }

    @Test
    fun `BatchUploadRequest equals works`() {
        val r1 = BatchUploadRequest(emptyList(), "device", 12345L)
        val r2 = BatchUploadRequest(emptyList(), "device", 12345L)
        assertEquals(r1, r2)
    }

    // ========== BatchUploadResponse Tests ==========

    @Test
    fun `BatchUploadResponse success can be instantiated`() {
        val response = BatchUploadResponse(success = true, processedCount = 10)
        assertNotNull(response)
        assertTrue(response.success)
        assertEquals(10, response.processedCount)
    }

    @Test
    fun `BatchUploadResponse failure has correct fields`() {
        val response = BatchUploadResponse(
            success = false,
            processedCount = 3,
            failedCount = 2,
            message = "Partial failure",
            failedEventIds = listOf("event-1", "event-2")
        )
        assertFalse(response.success)
        assertEquals(3, response.processedCount)
        assertEquals(2, response.failedCount)
        assertEquals("Partial failure", response.message)
        assertEquals(listOf("event-1", "event-2"), response.failedEventIds)
    }

    @Test
    fun `BatchUploadResponse defaults failedCount to 0`() {
        val response = BatchUploadResponse(success = true, processedCount = 5)
        assertEquals(0, response.failedCount)
    }

    @Test
    fun `BatchUploadResponse defaults message to null`() {
        val response = BatchUploadResponse(success = true, processedCount = 5)
        assertNull(response.message)
    }

    @Test
    fun `BatchUploadResponse defaults failedEventIds to empty`() {
        val response = BatchUploadResponse(success = true, processedCount = 5)
        assertTrue(response.failedEventIds.isEmpty())
    }

    @Test
    fun `BatchUploadResponse copy changes success`() {
        val response = BatchUploadResponse(success = true, processedCount = 5)
        val failed = response.copy(success = false)
        assertFalse(failed.success)
        assertEquals(5, failed.processedCount)
    }

    @Test
    fun `BatchUploadResponse equals works`() {
        val r1 = BatchUploadResponse(success = true, processedCount = 10)
        val r2 = BatchUploadResponse(success = true, processedCount = 10)
        assertEquals(r1, r2)
    }
}

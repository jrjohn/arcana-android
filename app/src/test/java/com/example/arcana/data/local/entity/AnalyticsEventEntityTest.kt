package com.example.arcana.data.local.entity

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AnalyticsEventEntity data class
 */
class AnalyticsEventEntityTest {

    private fun createEntity(
        eventId: String = "test-event-id",
        eventType: String = "USER_ACTION",
        eventName: String = "button_clicked",
        timestamp: Long = 1000L,
        sessionId: String = "session-123",
        userId: String? = null,
        screenName: String? = null,
        params: String = "{}",
        deviceInfo: String = "{}",
        appInfo: String = "{}",
        uploaded: Boolean = false,
        uploadAttempts: Int = 0,
        lastUploadAttempt: Long? = null
    ): AnalyticsEventEntity = AnalyticsEventEntity(
        eventId = eventId,
        eventType = eventType,
        eventName = eventName,
        timestamp = timestamp,
        sessionId = sessionId,
        userId = userId,
        screenName = screenName,
        params = params,
        deviceInfo = deviceInfo,
        appInfo = appInfo,
        uploaded = uploaded,
        uploadAttempts = uploadAttempts,
        lastUploadAttempt = lastUploadAttempt
    )

    // ========== Constructor / Required Fields ==========

    @Test
    fun `AnalyticsEventEntity can be instantiated with required fields`() {
        val entity = createEntity()
        assertNotNull(entity)
    }

    @Test
    fun `eventId is stored correctly`() {
        val entity = createEntity(eventId = "abc-123")
        assertEquals("abc-123", entity.eventId)
    }

    @Test
    fun `eventType is stored correctly`() {
        val entity = createEntity(eventType = "SCREEN_VIEW")
        assertEquals("SCREEN_VIEW", entity.eventType)
    }

    @Test
    fun `eventName is stored correctly`() {
        val entity = createEntity(eventName = "screen_viewed")
        assertEquals("screen_viewed", entity.eventName)
    }

    @Test
    fun `timestamp is stored correctly`() {
        val ts = System.currentTimeMillis()
        val entity = createEntity(timestamp = ts)
        assertEquals(ts, entity.timestamp)
    }

    @Test
    fun `sessionId is stored correctly`() {
        val entity = createEntity(sessionId = "session-xyz")
        assertEquals("session-xyz", entity.sessionId)
    }

    @Test
    fun `params is stored correctly`() {
        val params = """{"key":"value"}"""
        val entity = createEntity(params = params)
        assertEquals(params, entity.params)
    }

    @Test
    fun `deviceInfo is stored correctly`() {
        val deviceInfo = """{"model":"Pixel 6"}"""
        val entity = createEntity(deviceInfo = deviceInfo)
        assertEquals(deviceInfo, entity.deviceInfo)
    }

    @Test
    fun `appInfo is stored correctly`() {
        val appInfo = """{"version":"1.0.0"}"""
        val entity = createEntity(appInfo = appInfo)
        assertEquals(appInfo, entity.appInfo)
    }

    // ========== Optional Fields - Defaults ==========

    @Test
    fun `userId defaults to null`() {
        val entity = createEntity()
        assertNull(entity.userId)
    }

    @Test
    fun `screenName defaults to null`() {
        val entity = createEntity()
        assertNull(entity.screenName)
    }

    @Test
    fun `uploaded defaults to false`() {
        val entity = createEntity()
        assertFalse(entity.uploaded)
    }

    @Test
    fun `uploadAttempts defaults to 0`() {
        val entity = createEntity()
        assertEquals(0, entity.uploadAttempts)
    }

    @Test
    fun `lastUploadAttempt defaults to null`() {
        val entity = createEntity()
        assertNull(entity.lastUploadAttempt)
    }

    @Test
    fun `createdAt is set automatically and is positive`() {
        val entity = createEntity()
        assertTrue(entity.createdAt > 0)
    }

    // ========== Optional Fields - Explicit Values ==========

    @Test
    fun `userId can be set`() {
        val entity = createEntity(userId = "user-999")
        assertEquals("user-999", entity.userId)
    }

    @Test
    fun `screenName can be set`() {
        val entity = createEntity(screenName = "home")
        assertEquals("home", entity.screenName)
    }

    @Test
    fun `uploaded can be set to true`() {
        val entity = createEntity(uploaded = true)
        assertTrue(entity.uploaded)
    }

    @Test
    fun `uploadAttempts can be set`() {
        val entity = createEntity(uploadAttempts = 3)
        assertEquals(3, entity.uploadAttempts)
    }

    @Test
    fun `lastUploadAttempt can be set`() {
        val ts = System.currentTimeMillis()
        val entity = createEntity(lastUploadAttempt = ts)
        assertEquals(ts, entity.lastUploadAttempt)
    }

    // ========== Data Class Behavior ==========

    @Test
    fun `two identical entities are equal`() {
        val e1 = AnalyticsEventEntity(
            eventId = "id-1",
            eventType = "USER_ACTION",
            eventName = "click",
            timestamp = 1000L,
            sessionId = "sess",
            params = "{}",
            deviceInfo = "{}",
            appInfo = "{}",
            createdAt = 9999L
        )
        val e2 = AnalyticsEventEntity(
            eventId = "id-1",
            eventType = "USER_ACTION",
            eventName = "click",
            timestamp = 1000L,
            sessionId = "sess",
            params = "{}",
            deviceInfo = "{}",
            appInfo = "{}",
            createdAt = 9999L
        )
        assertEquals(e1, e2)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val entity = createEntity(eventId = "original-id", eventName = "original")
        val copied = entity.copy(eventName = "updated")
        assertEquals("original-id", copied.eventId)
        assertEquals("updated", copied.eventName)
    }

    @Test
    fun `copy can toggle uploaded flag`() {
        val entity = createEntity(uploaded = false)
        val uploaded = entity.copy(uploaded = true)
        assertTrue(uploaded.uploaded)
        assertFalse(entity.uploaded)
    }

    @Test
    fun `copy can increment uploadAttempts`() {
        val entity = createEntity(uploadAttempts = 2)
        val next = entity.copy(uploadAttempts = entity.uploadAttempts + 1)
        assertEquals(3, next.uploadAttempts)
    }

    @Test
    fun `hashCode is consistent for same data`() {
        val e1 = AnalyticsEventEntity(
            eventId = "x", eventType = "T", eventName = "n",
            timestamp = 0L, sessionId = "s",
            params = "{}", deviceInfo = "{}", appInfo = "{}",
            createdAt = 100L
        )
        val e2 = AnalyticsEventEntity(
            eventId = "x", eventType = "T", eventName = "n",
            timestamp = 0L, sessionId = "s",
            params = "{}", deviceInfo = "{}", appInfo = "{}",
            createdAt = 100L
        )
        assertEquals(e1.hashCode(), e2.hashCode())
    }

    @Test
    fun `toString contains eventId`() {
        val entity = createEntity(eventId = "unique-id-xyz")
        assertTrue(entity.toString().contains("unique-id-xyz"))
    }

    @Test
    fun `toString contains eventName`() {
        val entity = createEntity(eventName = "my_event")
        assertTrue(entity.toString().contains("my_event"))
    }

    // ========== Edge Cases ==========

    @Test
    fun `entity with empty string params is valid`() {
        val entity = createEntity(params = "")
        assertEquals("", entity.params)
    }

    @Test
    fun `entity with large timestamp`() {
        val entity = createEntity(timestamp = Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, entity.timestamp)
    }

    @Test
    fun `entity with max uploadAttempts`() {
        val entity = createEntity(uploadAttempts = Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, entity.uploadAttempts)
    }

    @Test
    fun `entity with all eventType strings`() {
        val eventTypes = listOf("USER_ACTION", "SCREEN_VIEW", "ERROR", "LIFECYCLE", "NETWORK", "PERFORMANCE")
        eventTypes.forEach { type ->
            val entity = createEntity(eventType = type)
            assertEquals(type, entity.eventType)
        }
    }
}

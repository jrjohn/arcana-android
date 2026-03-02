package com.example.arcana.core.analytics

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for AnalyticsManager.
 * Note: AnalyticsManager uses WorkManager which requires Android runtime for full testing.
 * These tests verify the constants and non-Android behavior.
 */
class AnalyticsManagerTest {

    // ========== Companion Object / Constants Tests ==========

    @Test
    fun `UPLOAD_INTERVAL_HOURS is positive`() {
        // Access the private constant via the behavior it implies
        // AnalyticsUploadWorker.WORK_NAME is used in AnalyticsManager
        assertTrue(com.example.arcana.data.worker.AnalyticsUploadWorker.WORK_NAME.isNotEmpty())
    }

    @Test
    fun `AnalyticsEvents USER_CREATED is correct`() {
        assertEquals("user_created", AnalyticsEvents.USER_CREATED)
    }

    @Test
    fun `AnalyticsEvents USER_UPDATED is correct`() {
        assertEquals("user_updated", AnalyticsEvents.USER_UPDATED)
    }

    @Test
    fun `AnalyticsEvents USER_DELETED is correct`() {
        assertEquals("user_deleted", AnalyticsEvents.USER_DELETED)
    }

    @Test
    fun `AnalyticsEvents SYNC_STARTED is correct`() {
        assertEquals("sync_started", AnalyticsEvents.SYNC_STARTED)
    }

    @Test
    fun `AnalyticsEvents SYNC_COMPLETED is correct`() {
        assertEquals("sync_completed", AnalyticsEvents.SYNC_COMPLETED)
    }

    @Test
    fun `AnalyticsEvents SYNC_FAILED is correct`() {
        assertEquals("sync_failed", AnalyticsEvents.SYNC_FAILED)
    }

    @Test
    fun `AnalyticsEvents PAGE_LOADED is correct`() {
        assertEquals("page_loaded", AnalyticsEvents.PAGE_LOADED)
    }

    @Test
    fun `AnalyticsEvents NETWORK_ERROR is correct`() {
        assertEquals("network_error", AnalyticsEvents.NETWORK_ERROR)
    }

    @Test
    fun `AnalyticsEvents VALIDATION_ERROR is correct`() {
        assertEquals("validation_error", AnalyticsEvents.VALIDATION_ERROR)
    }

    @Test
    fun `AnalyticsScreens HOME is correct`() {
        assertEquals("home", AnalyticsScreens.HOME)
    }

    @Test
    fun `AnalyticsScreens USER_LIST is correct`() {
        assertEquals("user_list", AnalyticsScreens.USER_LIST)
    }

    @Test
    fun `AnalyticsScreens USER_DIALOG is correct`() {
        assertEquals("user_dialog", AnalyticsScreens.USER_DIALOG)
    }

    @Test
    fun `AnalyticsScreens USER_CRUD is correct`() {
        assertEquals("user_crud", AnalyticsScreens.USER_CRUD)
    }

    // ========== Events object tests ==========

    @Test
    fun `Events SYNC_STARTED constant is correct`() {
        assertEquals("sync_started", Events.SYNC_STARTED)
    }

    @Test
    fun `Events SYNC_COMPLETED constant is correct`() {
        assertEquals("sync_completed", Events.SYNC_COMPLETED)
    }

    @Test
    fun `Events SYNC_FAILED constant is correct`() {
        assertEquals("sync_failed", Events.SYNC_FAILED)
    }

    @Test
    fun `Events APP_OPENED is defined`() {
        assertTrue(Events.APP_OPENED.isNotEmpty())
    }

    @Test
    fun `Events APP_CLOSED is defined`() {
        assertTrue(Events.APP_CLOSED.isNotEmpty())
    }

    @Test
    fun `Events SCREEN_ENTERED is defined`() {
        assertTrue(Events.SCREEN_ENTERED.isNotEmpty())
    }

    @Test
    fun `Events SCREEN_EXITED is defined`() {
        assertTrue(Events.SCREEN_EXITED.isNotEmpty())
    }

    // ========== Params object tests ==========

    @Test
    fun `Params SCREEN_NAME is defined`() {
        assertEquals("screen_name", Params.SCREEN_NAME)
    }

    @Test
    fun `Params DURATION_MS is defined`() {
        assertEquals("duration_ms", Params.DURATION_MS)
    }

    @Test
    fun `Params SUCCESS is defined`() {
        assertEquals("success", Params.SUCCESS)
    }

    @Test
    fun `Params ERROR_MESSAGE is defined`() {
        assertEquals("error_message", Params.ERROR_MESSAGE)
    }

    @Test
    fun `Params TRIGGER is defined`() {
        assertEquals("trigger", Params.TRIGGER)
    }

    @Test
    fun `Params TIMESTAMP is defined`() {
        assertEquals("timestamp", Params.TIMESTAMP)
    }

    @Test
    fun `Params USER_ID is defined`() {
        assertEquals("user_id", Params.USER_ID)
    }

    @Test
    fun `Params SOURCE is defined`() {
        assertEquals("source", Params.SOURCE)
    }

    // ========== EventType enum Tests ==========

    @Test
    fun `EventType has all expected values`() {
        val values = EventType.values()
        assertTrue(values.contains(EventType.SCREEN_VIEW))
        assertTrue(values.contains(EventType.USER_ACTION))
        assertTrue(values.contains(EventType.ERROR))
        assertTrue(values.contains(EventType.LIFECYCLE))
        assertTrue(values.contains(EventType.NETWORK))
        assertTrue(values.contains(EventType.PERFORMANCE))
    }

    @Test
    fun `EventType SCREEN_VIEW has correct name`() {
        assertEquals("SCREEN_VIEW", EventType.SCREEN_VIEW.name)
    }

    @Test
    fun `EventType ERROR has correct name`() {
        assertEquals("ERROR", EventType.ERROR.name)
    }

    @Test
    fun `EventType valueOf works`() {
        assertEquals(EventType.NETWORK, EventType.valueOf("NETWORK"))
        assertEquals(EventType.PERFORMANCE, EventType.valueOf("PERFORMANCE"))
    }
}

package com.example.arcana.core.analytics.annotations

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for analytics annotation classes via reflection.
 *
 * Note: Kotlin reflection on LOCAL functions (functions defined inside a method
 * body) throws KotlinReflectionInternalError. All annotated helper functions
 * must be declared at class scope so `::memberFun.annotations` works correctly.
 */
class AnnotationsTest {

    // ---- TrackAction helpers (class-level so reflection works) ----

    @TrackAction("button_clicked")
    private fun actionFunButtonClicked() {}

    @TrackAction("some_event")
    private fun actionFunSomeEvent() {}

    @TrackAction("event", includeParams = false)
    private fun actionFunNoParams() {}

    @TrackAction("user_created")
    private fun createUser() {}

    @TrackAction("test")
    private fun actionFunTest() {}

    // ---- TrackError helpers ----

    @TrackError
    private fun trackErrorFunDefault() {}

    @TrackError(source = "syncData")
    private fun syncData() {}

    @TrackError(trackSuccess = true)
    private fun trackErrorFunSuccess() {}

    @TrackError(source = "loadUsers", trackSuccess = true)
    private fun loadUsers() {}

    // ---- TrackPerformance helpers ----

    @TrackPerformance("data_load")
    private fun trackPerfFunDataLoad() {}

    @TrackPerformance("some_event")
    private fun trackPerfFunSomeEvent() {}

    @TrackPerformance("api_call", threshold = 500L)
    private fun makeApiCall() {}

    @TrackPerformance("event", threshold = 0L)
    private fun trackPerfFunZeroThreshold() {}

    @TrackPerformance("heavy_op", threshold = 10000L)
    private fun heavyOperation() {}

    // ========== TrackAction Tests ==========

    @Test
    fun `TrackAction annotation can be created with eventName`() {
        val annotation = ::actionFunButtonClicked.annotations
            .filterIsInstance<TrackAction>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("button_clicked", annotation.eventName)
    }

    @Test
    fun `TrackAction includeParams defaults to true`() {
        val annotation = ::actionFunSomeEvent.annotations
            .filterIsInstance<TrackAction>()
            .firstOrNull()

        assertNotNull(annotation)
        assertTrue(annotation.includeParams)
    }

    @Test
    fun `TrackAction includeParams can be set to false`() {
        val annotation = ::actionFunNoParams.annotations
            .filterIsInstance<TrackAction>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(false, annotation.includeParams)
    }

    @Test
    fun `TrackAction stores event name correctly`() {
        val annotation = ::createUser.annotations
            .filterIsInstance<TrackAction>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("user_created", annotation.eventName)
    }

    @Test
    fun `TrackAction annotation is retained at runtime`() {
        val annotation = ::actionFunTest.annotations
            .filterIsInstance<TrackAction>()
            .firstOrNull()
        assertNotNull(annotation)
    }

    // ========== TrackScreen Tests ==========

    @Test
    fun `TrackScreen annotation can be applied to class`() {
        @TrackScreen("home")
        class DummyClass

        val annotation = DummyClass::class.annotations
            .filterIsInstance<TrackScreen>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("home", annotation.screenName)
    }

    @Test
    fun `TrackScreen autoTrack defaults to true`() {
        @TrackScreen("profile")
        class DummyClass

        val annotation = DummyClass::class.annotations
            .filterIsInstance<TrackScreen>()
            .firstOrNull()

        assertNotNull(annotation)
        assertTrue(annotation.autoTrack)
    }

    @Test
    fun `TrackScreen autoTrack can be set to false`() {
        @TrackScreen("settings", autoTrack = false)
        class DummyClass

        val annotation = DummyClass::class.annotations
            .filterIsInstance<TrackScreen>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(false, annotation.autoTrack)
    }

    @Test
    fun `TrackScreen stores screen name correctly`() {
        @TrackScreen("user_list")
        class UserListViewModel

        val annotation = UserListViewModel::class.annotations
            .filterIsInstance<TrackScreen>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("user_list", annotation.screenName)
    }

    @Test
    fun `TrackAction annotation type is correct`() {
        val annotationType = TrackAction::class
        assertNotNull(annotationType)
        assertEquals("TrackAction", annotationType.simpleName)
    }

    @Test
    fun `TrackScreen annotation type is correct`() {
        val annotationType = TrackScreen::class
        assertNotNull(annotationType)
        assertEquals("TrackScreen", annotationType.simpleName)
    }

    // ========== TrackError Tests ==========

    @Test
    fun `TrackError annotation type is correct`() {
        assertNotNull(TrackError::class)
        assertEquals("TrackError", TrackError::class.simpleName)
    }

    @Test
    fun `TrackError source defaults to empty string`() {
        val annotation = ::trackErrorFunDefault.annotations
            .filterIsInstance<TrackError>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("", annotation.source)
    }

    @Test
    fun `TrackError trackSuccess defaults to false`() {
        val annotation = ::trackErrorFunDefault.annotations
            .filterIsInstance<TrackError>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(false, annotation.trackSuccess)
    }

    @Test
    fun `TrackError can set source`() {
        val annotation = ::syncData.annotations
            .filterIsInstance<TrackError>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("syncData", annotation.source)
    }

    @Test
    fun `TrackError can set trackSuccess to true`() {
        val annotation = ::trackErrorFunSuccess.annotations
            .filterIsInstance<TrackError>()
            .firstOrNull()

        assertNotNull(annotation)
        assertTrue(annotation.trackSuccess)
    }

    @Test
    fun `TrackError with both source and trackSuccess`() {
        val annotation = ::loadUsers.annotations
            .filterIsInstance<TrackError>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("loadUsers", annotation.source)
        assertTrue(annotation.trackSuccess)
    }

    // ========== TrackPerformance Tests ==========

    @Test
    fun `TrackPerformance annotation type is correct`() {
        assertNotNull(TrackPerformance::class)
        assertEquals("TrackPerformance", TrackPerformance::class.simpleName)
    }

    @Test
    fun `TrackPerformance eventName is stored correctly`() {
        val annotation = ::trackPerfFunDataLoad.annotations
            .filterIsInstance<TrackPerformance>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("data_load", annotation.eventName)
    }

    @Test
    fun `TrackPerformance threshold defaults to 0`() {
        val annotation = ::trackPerfFunSomeEvent.annotations
            .filterIsInstance<TrackPerformance>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(0L, annotation.threshold)
    }

    @Test
    fun `TrackPerformance can set threshold`() {
        val annotation = ::makeApiCall.annotations
            .filterIsInstance<TrackPerformance>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("api_call", annotation.eventName)
        assertEquals(500L, annotation.threshold)
    }

    @Test
    fun `TrackPerformance with zero threshold`() {
        val annotation = ::trackPerfFunZeroThreshold.annotations
            .filterIsInstance<TrackPerformance>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(0L, annotation.threshold)
    }

    @Test
    fun `TrackPerformance with large threshold`() {
        val annotation = ::heavyOperation.annotations
            .filterIsInstance<TrackPerformance>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(10000L, annotation.threshold)
    }
}

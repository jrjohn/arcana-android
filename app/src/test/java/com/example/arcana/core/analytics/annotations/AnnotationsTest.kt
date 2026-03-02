package com.example.arcana.core.analytics.annotations

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for analytics annotation classes via reflection
 */
class AnnotationsTest {

    // ========== TrackAction Tests ==========

    @Test
    fun `TrackAction annotation can be created with eventName`() {
        @TrackAction("button_clicked")
        fun dummyFun() {}

        val annotation = ::dummyFun.annotations
            .filterIsInstance<TrackAction>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("button_clicked", annotation.eventName)
    }

    @Test
    fun `TrackAction includeParams defaults to true`() {
        @TrackAction("some_event")
        fun dummyFun() {}

        val annotation = ::dummyFun.annotations
            .filterIsInstance<TrackAction>()
            .firstOrNull()

        assertNotNull(annotation)
        assertTrue(annotation.includeParams)
    }

    @Test
    fun `TrackAction includeParams can be set to false`() {
        @TrackAction("event", includeParams = false)
        fun dummyFun() {}

        val annotation = ::dummyFun.annotations
            .filterIsInstance<TrackAction>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(false, annotation.includeParams)
    }

    @Test
    fun `TrackAction stores event name correctly`() {
        @TrackAction("user_created")
        fun createUser() {}

        val annotation = ::createUser.annotations
            .filterIsInstance<TrackAction>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("user_created", annotation.eventName)
    }

    @Test
    fun `TrackAction annotation is retained at runtime`() {
        val retention = TrackAction::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()

        // Kotlin annotations with RUNTIME retention should be accessible
        @TrackAction("test")
        fun fn() {}
        assertNotNull(::fn.annotations.filterIsInstance<TrackAction>().firstOrNull())
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

    // ========== TrackError Tests (if exists) ==========

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
        @TrackError
        fun dummyFun() {}

        val annotation = ::dummyFun.annotations
            .filterIsInstance<TrackError>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("", annotation.source)
    }

    @Test
    fun `TrackError trackSuccess defaults to false`() {
        @TrackError
        fun dummyFun() {}

        val annotation = ::dummyFun.annotations
            .filterIsInstance<TrackError>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(false, annotation.trackSuccess)
    }

    @Test
    fun `TrackError can set source`() {
        @TrackError(source = "syncData")
        fun syncData() {}

        val annotation = ::syncData.annotations
            .filterIsInstance<TrackError>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("syncData", annotation.source)
    }

    @Test
    fun `TrackError can set trackSuccess to true`() {
        @TrackError(trackSuccess = true)
        fun dummyFun() {}

        val annotation = ::dummyFun.annotations
            .filterIsInstance<TrackError>()
            .firstOrNull()

        assertNotNull(annotation)
        assertTrue(annotation.trackSuccess)
    }

    @Test
    fun `TrackError with both source and trackSuccess`() {
        @TrackError(source = "loadUsers", trackSuccess = true)
        fun loadUsers() {}

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
        @TrackPerformance("data_load")
        fun dummyFun() {}

        val annotation = ::dummyFun.annotations
            .filterIsInstance<TrackPerformance>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("data_load", annotation.eventName)
    }

    @Test
    fun `TrackPerformance threshold defaults to 0`() {
        @TrackPerformance("some_event")
        fun dummyFun() {}

        val annotation = ::dummyFun.annotations
            .filterIsInstance<TrackPerformance>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(0L, annotation.threshold)
    }

    @Test
    fun `TrackPerformance can set threshold`() {
        @TrackPerformance("api_call", threshold = 500L)
        fun makeApiCall() {}

        val annotation = ::makeApiCall.annotations
            .filterIsInstance<TrackPerformance>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals("api_call", annotation.eventName)
        assertEquals(500L, annotation.threshold)
    }

    @Test
    fun `TrackPerformance with zero threshold`() {
        @TrackPerformance("event", threshold = 0L)
        fun dummyFun() {}

        val annotation = ::dummyFun.annotations
            .filterIsInstance<TrackPerformance>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(0L, annotation.threshold)
    }

    @Test
    fun `TrackPerformance with large threshold`() {
        @TrackPerformance("heavy_op", threshold = 10000L)
        fun heavyOperation() {}

        val annotation = ::heavyOperation.annotations
            .filterIsInstance<TrackPerformance>()
            .firstOrNull()

        assertNotNull(annotation)
        assertEquals(10000L, annotation.threshold)
    }
}

package com.example.arcana.core.analytics

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AnalyticsExtensionsTest {

    private val mockTracker: AnalyticsTracker = mock()

    // ========== trackInteraction Tests ==========

    @Test
    fun `trackInteraction calls trackEvent with correct event name`() {
        val eventName = "button_clicked"
        trackInteraction(mockTracker, eventName)
        verify(mockTracker).trackEvent(eventName, emptyMap())
    }

    @Test
    fun `trackInteraction calls trackEvent with params`() {
        val eventName = "button_clicked"
        val params = mapOf("button_name" to "Submit")
        trackInteraction(mockTracker, eventName, params)
        verify(mockTracker).trackEvent(eventName, params)
    }

    @Test
    fun `trackInteraction with empty params calls trackEvent with empty map`() {
        val eventName = "some_event"
        trackInteraction(mockTracker, eventName, emptyMap())
        verify(mockTracker).trackEvent(eventName, emptyMap())
    }

    @Test
    fun `trackInteraction does not throw when tracker throws`() {
        val eventName = "bad_event"
        whenever(mockTracker.trackEvent(any(), any())).thenThrow(RuntimeException("tracker error"))
        // Should not throw - exception is swallowed
        trackInteraction(mockTracker, eventName)
    }

    // ========== CrudOperation Tests ==========

    @Test
    fun `CrudOperation enum has all expected values`() {
        val values = CrudOperation.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(CrudOperation.CREATE))
        assertTrue(values.contains(CrudOperation.READ))
        assertTrue(values.contains(CrudOperation.UPDATE))
        assertTrue(values.contains(CrudOperation.DELETE))
    }

    @Test
    fun `CrudOperation CREATE has correct name`() {
        assertEquals("CREATE", CrudOperation.CREATE.name)
    }

    @Test
    fun `CrudOperation READ has correct name`() {
        assertEquals("READ", CrudOperation.READ.name)
    }

    @Test
    fun `CrudOperation UPDATE has correct name`() {
        assertEquals("UPDATE", CrudOperation.UPDATE.name)
    }

    @Test
    fun `CrudOperation DELETE has correct name`() {
        assertEquals("DELETE", CrudOperation.DELETE.name)
    }

    @Test
    fun `CrudOperation valueOf works for all values`() {
        assertEquals(CrudOperation.CREATE, CrudOperation.valueOf("CREATE"))
        assertEquals(CrudOperation.READ, CrudOperation.valueOf("READ"))
        assertEquals(CrudOperation.UPDATE, CrudOperation.valueOf("UPDATE"))
        assertEquals(CrudOperation.DELETE, CrudOperation.valueOf("DELETE"))
    }

    // ========== trackSync Tests ==========

    @Test
    fun `trackSync tracks SYNC_STARTED event`() = runTest {
        trackSync(mockTracker, "home", "manual") { "result" }
        verify(mockTracker).trackEvent(
            argThat { this == Events.SYNC_STARTED },
            argThat { this[Params.SCREEN_NAME] == "home" && this[Params.TRIGGER] == "manual" }
        )
    }

    @Test
    fun `trackSync tracks SYNC_COMPLETED event on success`() = runTest {
        trackSync(mockTracker, "home", "auto") { "result" }
        verify(mockTracker).trackEvent(
            argThat { this == Events.SYNC_COMPLETED },
            argThat { this[Params.SCREEN_NAME] == "home" && this[Params.SUCCESS] == "true" }
        )
    }

    @Test
    fun `trackSync returns block result on success`() = runTest {
        val result = trackSync(mockTracker, "home") { 42 }
        assertEquals(42, result)
    }

    @Test
    fun `trackSync uses manual trigger by default`() = runTest {
        trackSync(mockTracker, "screen") { "ok" }
        verify(mockTracker).trackEvent(
            argThat { this == Events.SYNC_STARTED },
            argThat { this[Params.TRIGGER] == "manual" }
        )
    }

    @Test
    fun `trackSync tracks SYNC_FAILED event on exception`() = runTest {
        val exception = RuntimeException("sync error")
        assertFailsWith<RuntimeException> {
            trackSync(mockTracker, "home") { throw exception }
        }
        verify(mockTracker).trackEvent(
            argThat { this == Events.SYNC_FAILED },
            argThat {
                this[Params.SCREEN_NAME] == "home" &&
                this[Params.ERROR_MESSAGE] == "sync error"
            }
        )
    }

    @Test
    fun `trackSync tracks error on exception`() = runTest {
        val exception = RuntimeException("sync error")
        assertFailsWith<RuntimeException> {
            trackSync(mockTracker, "home") { throw exception }
        }
        verify(mockTracker).trackError(any(), any())
    }

    @Test
    fun `trackSync rethrows exception`() = runTest {
        val exception = RuntimeException("sync failed")
        assertFailsWith<RuntimeException> {
            trackSync(mockTracker, "home") { throw exception }
        }
    }

    @Test
    fun `trackSync includes duration in SYNC_COMPLETED params`() = runTest {
        trackSync(mockTracker, "screen") { "done" }
        verify(mockTracker).trackEvent(
            argThat { this == Events.SYNC_COMPLETED },
            argThat { containsKey(Params.DURATION_MS) }
        )
    }

    @Test
    fun `trackSync includes duration in SYNC_FAILED params`() = runTest {
        assertFailsWith<RuntimeException> {
            trackSync(mockTracker, "screen") { throw RuntimeException("fail") }
        }
        verify(mockTracker).trackEvent(
            argThat { this == Events.SYNC_FAILED },
            argThat { containsKey(Params.DURATION_MS) }
        )
    }

    // ========== trackCrudOperation Tests ==========

    @Test
    fun `trackCrudOperation CREATE tracks success event`() = runTest {
        trackCrudOperation(mockTracker, CrudOperation.CREATE, "User") { "created" }
        verify(mockTracker).trackEvent(
            argThat { this == "USER_CREATE_SUCCESS" },
            argThat { this[Params.SUCCESS] == "true" }
        )
    }

    @Test
    fun `trackCrudOperation READ tracks success event`() = runTest {
        trackCrudOperation(mockTracker, CrudOperation.READ, "User") { "read" }
        verify(mockTracker).trackEvent(
            argThat { this == "USER_READ_SUCCESS" },
            argThat { this[Params.SUCCESS] == "true" }
        )
    }

    @Test
    fun `trackCrudOperation UPDATE tracks success event`() = runTest {
        trackCrudOperation(mockTracker, CrudOperation.UPDATE, "User") { "updated" }
        verify(mockTracker).trackEvent(
            argThat { this == "USER_UPDATE_SUCCESS" },
            argThat { this[Params.SUCCESS] == "true" }
        )
    }

    @Test
    fun `trackCrudOperation DELETE tracks success event`() = runTest {
        trackCrudOperation(mockTracker, CrudOperation.DELETE, "User") { "deleted" }
        verify(mockTracker).trackEvent(
            argThat { this == "USER_DELETE_SUCCESS" },
            argThat { this[Params.SUCCESS] == "true" }
        )
    }

    @Test
    fun `trackCrudOperation CREATE tracks failure event on exception`() = runTest {
        val exception = RuntimeException("create failed")
        assertFailsWith<RuntimeException> {
            trackCrudOperation(mockTracker, CrudOperation.CREATE, "User") { throw exception }
        }
        verify(mockTracker).trackEvent(
            argThat { this == "USER_CREATE_FAILED" },
            argThat { this[Params.ERROR_MESSAGE] == "create failed" }
        )
    }

    @Test
    fun `trackCrudOperation READ tracks failure event on exception`() = runTest {
        assertFailsWith<RuntimeException> {
            trackCrudOperation(mockTracker, CrudOperation.READ, "User") {
                throw RuntimeException("read failed")
            }
        }
        verify(mockTracker).trackEvent(
            argThat { this == "USER_READ_FAILED" },
            any()
        )
    }

    @Test
    fun `trackCrudOperation UPDATE tracks failure event on exception`() = runTest {
        assertFailsWith<RuntimeException> {
            trackCrudOperation(mockTracker, CrudOperation.UPDATE, "User") {
                throw RuntimeException("update failed")
            }
        }
        verify(mockTracker).trackEvent(
            argThat { this == "USER_UPDATE_FAILED" },
            any()
        )
    }

    @Test
    fun `trackCrudOperation DELETE tracks failure event on exception`() = runTest {
        assertFailsWith<RuntimeException> {
            trackCrudOperation(mockTracker, CrudOperation.DELETE, "User") {
                throw RuntimeException("delete failed")
            }
        }
        verify(mockTracker).trackEvent(
            argThat { this == "USER_DELETE_FAILED" },
            any()
        )
    }

    @Test
    fun `trackCrudOperation returns block result`() = runTest {
        val result = trackCrudOperation(mockTracker, CrudOperation.READ, "User") { 99 }
        assertEquals(99, result)
    }

    @Test
    fun `trackCrudOperation rethrows exception`() = runTest {
        val exception = RuntimeException("crud failed")
        assertFailsWith<RuntimeException> {
            trackCrudOperation(mockTracker, CrudOperation.CREATE, "User") { throw exception }
        }
    }

    @Test
    fun `trackCrudOperation tracks error on exception`() = runTest {
        assertFailsWith<RuntimeException> {
            trackCrudOperation(mockTracker, CrudOperation.CREATE, "User") {
                throw RuntimeException("create error")
            }
        }
        verify(mockTracker).trackError(any(), any())
    }

    @Test
    fun `trackCrudOperation includes duration in success params`() = runTest {
        trackCrudOperation(mockTracker, CrudOperation.CREATE, "User") { "done" }
        verify(mockTracker).trackEvent(
            any(),
            argThat { containsKey(Params.DURATION_MS) }
        )
    }

    @Test
    fun `trackCrudOperation includes params in success event`() = runTest {
        val params = mapOf("user_id" to "123")
        trackCrudOperation(mockTracker, CrudOperation.CREATE, "User", params) { "done" }
        verify(mockTracker).trackEvent(
            argThat { this == "USER_CREATE_SUCCESS" },
            argThat { this["user_id"] == "123" }
        )
    }

    @Test
    fun `trackCrudOperation entity name is uppercased in event name`() = runTest {
        trackCrudOperation(mockTracker, CrudOperation.CREATE, "user") { "done" }
        verify(mockTracker).trackEvent(
            argThat { this == "USER_CREATE_SUCCESS" },
            any()
        )
    }

    // ========== trackFlow Tests ==========

    @Test
    fun `trackFlow tracks success event on data emission`() = runTest {
        val testFlow = flow { emit("data1") }
        val results = testFlow.trackFlow(
            analyticsTracker = mockTracker,
            eventName = "data_loaded"
        ).toList()

        assertEquals(listOf("data1"), results)
        verify(mockTracker).trackEvent(
            argThat { this == "data_loaded" },
            argThat { this[Params.SUCCESS] == "true" }
        )
    }

    @Test
    fun `trackFlow tracks duration when trackPerformance is true`() = runTest {
        val testFlow = flow { emit("item") }
        testFlow.trackFlow(
            analyticsTracker = mockTracker,
            eventName = "loaded",
            trackPerformance = true
        ).toList()

        verify(mockTracker).trackEvent(
            any(),
            argThat { containsKey(Params.DURATION_MS) }
        )
    }

    @Test
    fun `trackFlow does not track duration when trackPerformance is false`() = runTest {
        val testFlow = flow { emit("item") }
        testFlow.trackFlow(
            analyticsTracker = mockTracker,
            eventName = "loaded",
            trackPerformance = false
        ).toList()

        verify(mockTracker).trackEvent(
            any(),
            argThat { !containsKey(Params.DURATION_MS) }
        )
    }

    @Test
    fun `trackFlow tracks error on flow exception`() = runTest {
        val exception = RuntimeException("flow error")
        val errorFlow = flow<String> { throw exception }

        assertFailsWith<RuntimeException> {
            errorFlow.trackFlow(
                analyticsTracker = mockTracker,
                eventName = "loaded"
            ).toList()
        }

        verify(mockTracker).trackEvent(
            argThat { this == "loaded" },
            argThat { this[Params.SUCCESS] == "false" }
        )
    }

    @Test
    fun `trackFlow includes error message in error params`() = runTest {
        val exception = RuntimeException("flow failure")
        val errorFlow = flow<String> { throw exception }

        assertFailsWith<RuntimeException> {
            errorFlow.trackFlow(
                analyticsTracker = mockTracker,
                eventName = "loaded"
            ).toList()
        }

        verify(mockTracker).trackEvent(
            any(),
            argThat { this[Params.ERROR_MESSAGE] == "flow failure" }
        )
    }

    @Test
    fun `trackFlow calls trackError on exception`() = runTest {
        val exception = RuntimeException("flow error")
        val errorFlow = flow<String> { throw exception }

        assertFailsWith<RuntimeException> {
            errorFlow.trackFlow(
                analyticsTracker = mockTracker,
                eventName = "loaded",
                trackErrors = true
            ).toList()
        }

        verify(mockTracker).trackError(any(), any())
    }

    @Test
    fun `trackFlow does not track error when trackErrors is false`() = runTest {
        val exception = RuntimeException("flow error")
        val errorFlow = flow<String> { throw exception }

        assertFailsWith<RuntimeException> {
            errorFlow.trackFlow(
                analyticsTracker = mockTracker,
                eventName = "loaded",
                trackErrors = false
            ).toList()
        }

        verify(mockTracker, never()).trackError(any(), any())
    }

    @Test
    fun `trackFlow passes extra params to tracker`() = runTest {
        val extraParams = mapOf(Params.SCREEN_NAME to "home")
        val testFlow = flow { emit("item") }

        testFlow.trackFlow(
            analyticsTracker = mockTracker,
            eventName = "loaded",
            params = extraParams
        ).toList()

        verify(mockTracker).trackEvent(
            any(),
            argThat { this[Params.SCREEN_NAME] == "home" }
        )
    }

    @Test
    fun `trackFlow invokes onData callback with data`() = runTest {
        val capturedData = mutableListOf<String>()
        val testFlow = flow { emit("hello") }

        testFlow.trackFlow(
            analyticsTracker = mockTracker,
            eventName = "loaded",
            onData = { data ->
                capturedData.add(data)
                mapOf("data_key" to data)
            }
        ).toList()

        assertEquals(listOf("hello"), capturedData)
        verify(mockTracker).trackEvent(
            any(),
            argThat { this["data_key"] == "hello" }
        )
    }

    @Test
    fun `trackFlow rethrows exception after tracking`() = runTest {
        val exception = RuntimeException("flow error")
        val errorFlow = flow<String> { throw exception }

        val thrown = assertFailsWith<RuntimeException> {
            errorFlow.trackFlow(mockTracker, "loaded").toList()
        }
        assertEquals("flow error", thrown.message)
    }

    @Test
    fun `trackFlow uses unknown error for null message`() = runTest {
        val exception = RuntimeException()  // null message
        val errorFlow = flow<String> { throw exception }

        assertFailsWith<RuntimeException> {
            errorFlow.trackFlow(mockTracker, "loaded").toList()
        }

        verify(mockTracker).trackEvent(
            any(),
            argThat { this[Params.ERROR_MESSAGE] == "Unknown error" }
        )
    }
}

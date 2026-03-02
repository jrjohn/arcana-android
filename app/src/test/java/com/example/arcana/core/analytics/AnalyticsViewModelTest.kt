package com.example.arcana.core.analytics

import com.example.arcana.core.analytics.annotations.TrackScreen
import com.example.arcana.core.common.AppError
import com.example.arcana.core.common.ErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockTracker: AnalyticsTracker

    // ---- concrete subclass for testing ----
    class TestViewModel(tracker: AnalyticsTracker) : AnalyticsViewModel(tracker) {
        fun doTrackEvent(name: String, params: Map<String, String> = emptyMap()) {
            trackEvent(name, params)
        }
        fun doTrackError(error: Throwable, params: Map<String, String> = emptyMap()) {
            trackError(error, params)
        }
        fun doTrackAppError(appError: AppError, params: Map<String, String> = emptyMap()) {
            trackAppError(appError, params)
        }
        suspend fun <T> doTrackPerformance(name: String, block: suspend () -> T): T {
            return trackPerformance(name, block = block)
        }
        suspend fun <T> doTrackAction(
            name: String,
            params: Map<String, String> = emptyMap(),
            trackPerf: Boolean = false,
            block: suspend () -> T
        ): T {
            return trackAction(name, params = params, trackPerformance = trackPerf, block = block)
        }
    }

    // ---- annotated subclass for auto-track testing ----
    @TrackScreen(screenName = "test_screen", autoTrack = true)
    class AnnotatedTestViewModel(tracker: AnalyticsTracker) : AnalyticsViewModel(tracker)

    // ---- annotated but autoTrack=false ----
    @TrackScreen(screenName = "disabled_screen", autoTrack = false)
    class DisabledAutoTrackViewModel(tracker: AnalyticsTracker) : AnalyticsViewModel(tracker)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockTracker = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Construction Tests ==========

    @Test
    fun `viewModel can be instantiated`() = runTest {
        val vm = TestViewModel(mockTracker)
        assertNotNull(vm)
    }

    @Test
    fun `non-annotated viewModel does not auto-track screen`() = runTest {
        val vm = TestViewModel(mockTracker)
        advanceUntilIdle()
        verify(mockTracker, never()).trackScreen(any(), any())
    }

    @Test
    fun `annotated viewModel auto-tracks screen on init`() = runTest {
        val vm = AnnotatedTestViewModel(mockTracker)
        advanceUntilIdle()
        verify(mockTracker).trackScreen(argThat { this == "test_screen" }, any())
    }

    @Test
    fun `annotated viewModel with autoTrack=false does not auto-track`() = runTest {
        val vm = DisabledAutoTrackViewModel(mockTracker)
        advanceUntilIdle()
        verify(mockTracker, never()).trackScreen(any(), any())
    }

    @Test
    fun `auto-track passes timestamp param`() = runTest {
        val vm = AnnotatedTestViewModel(mockTracker)
        advanceUntilIdle()
        verify(mockTracker).trackScreen(
            any(),
            argThat { containsKey(Params.TIMESTAMP) }
        )
    }

    // ========== trackEvent Tests ==========

    @Test
    fun `trackEvent calls analyticsTracker`() = runTest {
        val vm = TestViewModel(mockTracker)
        vm.doTrackEvent("button_clicked")
        advanceUntilIdle()
        verify(mockTracker).trackEvent("button_clicked", emptyMap())
    }

    @Test
    fun `trackEvent with params calls analyticsTracker`() = runTest {
        val vm = TestViewModel(mockTracker)
        val params = mapOf("button" to "submit", "screen" to "home")
        vm.doTrackEvent("button_clicked", params)
        advanceUntilIdle()
        verify(mockTracker).trackEvent("button_clicked", params)
    }

    @Test
    fun `trackEvent swallows exception from tracker`() = runTest {
        whenever(mockTracker.trackEvent(any(), any())).thenThrow(RuntimeException("tracker error"))
        val vm = TestViewModel(mockTracker)
        // Should not throw
        vm.doTrackEvent("test_event")
        advanceUntilIdle()
    }

    @Test
    fun `multiple trackEvent calls all invoke tracker`() = runTest {
        val vm = TestViewModel(mockTracker)
        vm.doTrackEvent("event1")
        vm.doTrackEvent("event2")
        vm.doTrackEvent("event3")
        advanceUntilIdle()
        verify(mockTracker).trackEvent("event1", emptyMap())
        verify(mockTracker).trackEvent("event2", emptyMap())
        verify(mockTracker).trackEvent("event3", emptyMap())
    }

    // ========== trackError Tests ==========

    @Test
    fun `trackError calls analyticsTracker`() = runTest {
        val vm = TestViewModel(mockTracker)
        val error = RuntimeException("test error")
        vm.doTrackError(error)
        advanceUntilIdle()
        verify(mockTracker).trackError(error, emptyMap())
    }

    @Test
    fun `trackError with params calls analyticsTracker`() = runTest {
        val vm = TestViewModel(mockTracker)
        val error = IllegalStateException("invalid state")
        val params = mapOf("screen" to "home")
        vm.doTrackError(error, params)
        advanceUntilIdle()
        verify(mockTracker).trackError(error, params)
    }

    @Test
    fun `trackError swallows exception from tracker`() = runTest {
        whenever(mockTracker.trackError(any(), any())).thenThrow(RuntimeException("tracker error"))
        val vm = TestViewModel(mockTracker)
        val error = RuntimeException("original error")
        // Should not throw
        vm.doTrackError(error)
        advanceUntilIdle()
    }

    // ========== trackAppError Tests ==========

    @Test
    fun `trackAppError calls analyticsTracker`() = runTest {
        val vm = TestViewModel(mockTracker)
        val appError = AppError.NetworkError(message = "Network failed")
        vm.doTrackAppError(appError)
        advanceUntilIdle()
        verify(mockTracker).trackAppError(appError, emptyMap())
    }

    @Test
    fun `trackAppError with params calls analyticsTracker`() = runTest {
        val vm = TestViewModel(mockTracker)
        val appError = AppError.ServerError(code = 500, message = "Server Error")
        val params = mapOf("endpoint" to "/api/users")
        vm.doTrackAppError(appError, params)
        advanceUntilIdle()
        verify(mockTracker).trackAppError(appError, params)
    }

    @Test
    fun `trackAppError swallows exception from tracker`() = runTest {
        whenever(mockTracker.trackAppError(any(), any())).thenThrow(RuntimeException("error"))
        val vm = TestViewModel(mockTracker)
        val appError = AppError.AuthError(message = "Unauthorized")
        // Should not throw
        vm.doTrackAppError(appError)
        advanceUntilIdle()
    }

    @Test
    fun `trackAppError with ValidationError`() = runTest {
        val vm = TestViewModel(mockTracker)
        val appError = AppError.ValidationError(field = "email", message = "Invalid email")
        vm.doTrackAppError(appError)
        advanceUntilIdle()
        verify(mockTracker).trackAppError(appError, emptyMap())
    }

    @Test
    fun `trackAppError with ConflictError`() = runTest {
        val vm = TestViewModel(mockTracker)
        val appError = AppError.ConflictError(message = "Conflict")
        vm.doTrackAppError(appError)
        advanceUntilIdle()
        verify(mockTracker).trackAppError(appError, emptyMap())
    }

    @Test
    fun `trackAppError with UnknownError`() = runTest {
        val vm = TestViewModel(mockTracker)
        val appError = AppError.UnknownError(throwable = Exception("unknown"))
        vm.doTrackAppError(appError)
        advanceUntilIdle()
        verify(mockTracker).trackAppError(appError, emptyMap())
    }

    // ========== trackPerformance Tests ==========

    @Test
    fun `trackPerformance executes block and returns result`() = runTest {
        val vm = TestViewModel(mockTracker)
        val result = vm.doTrackPerformance("load_data") { 42 }
        advanceUntilIdle()
        assertEquals(42, result)
    }

    @Test
    fun `trackPerformance tracks event with duration`() = runTest {
        val vm = TestViewModel(mockTracker)
        vm.doTrackPerformance("load_data") { "result" }
        advanceUntilIdle()
        verify(mockTracker).trackEvent(
            argThat { this == "load_data" },
            argThat { containsKey(Params.DURATION_MS) }
        )
    }

    @Test
    fun `trackPerformance tracks event even on exception`() = runTest {
        val vm = TestViewModel(mockTracker)
        try {
            vm.doTrackPerformance("failing_op") { throw RuntimeException("fail") }
        } catch (e: RuntimeException) {
            // expected
        }
        advanceUntilIdle()
        // The finally block should still fire
        verify(mockTracker).trackEvent(argThat { this == "failing_op" }, any())
    }

    @Test
    fun `trackPerformance with string result`() = runTest {
        val vm = TestViewModel(mockTracker)
        val result = vm.doTrackPerformance("fetch_users") { "users_list" }
        advanceUntilIdle()
        assertEquals("users_list", result)
    }

    // ========== trackAction Tests ==========

    @Test
    fun `trackAction success tracks event with success=true`() = runTest {
        val vm = TestViewModel(mockTracker)
        val result = vm.doTrackAction("create_user") { "user_created" }
        advanceUntilIdle()
        assertEquals("user_created", result)
        verify(mockTracker).trackEvent(
            argThat { this == "create_user" },
            argThat { this[Params.SUCCESS] == "true" }
        )
    }

    @Test
    fun `trackAction failure tracks event with success=false`() = runTest {
        val vm = TestViewModel(mockTracker)
        assertFailsWith<RuntimeException> {
            vm.doTrackAction("create_user") { throw RuntimeException("creation failed") }
        }
        advanceUntilIdle()
        verify(mockTracker).trackEvent(
            argThat { this == "create_user" },
            argThat {
                this[Params.SUCCESS] == "false" &&
                this[Params.ERROR_MESSAGE] == "creation failed"
            }
        )
    }

    @Test
    fun `trackAction failure also tracks error`() = runTest {
        val vm = TestViewModel(mockTracker)
        val error = RuntimeException("action failed")
        assertFailsWith<RuntimeException> {
            vm.doTrackAction("delete_user") { throw error }
        }
        advanceUntilIdle()
        verify(mockTracker).trackError(error, emptyMap())
    }

    @Test
    fun `trackAction failure rethrows exception`() = runTest {
        val vm = TestViewModel(mockTracker)
        val exception = IllegalStateException("illegal")
        val thrown = assertFailsWith<IllegalStateException> {
            vm.doTrackAction("some_action") { throw exception }
        }
        assertEquals("illegal", thrown.message)
    }

    @Test
    fun `trackAction with trackPerformance includes duration`() = runTest {
        val vm = TestViewModel(mockTracker)
        vm.doTrackAction("load_data", trackPerf = true) { "data" }
        advanceUntilIdle()
        verify(mockTracker).trackEvent(
            any(),
            argThat { containsKey(Params.DURATION_MS) && this[Params.SUCCESS] == "true" }
        )
    }

    @Test
    fun `trackAction failure with trackPerformance includes duration`() = runTest {
        val vm = TestViewModel(mockTracker)
        assertFailsWith<RuntimeException> {
            vm.doTrackAction("failing_action", trackPerf = true) {
                throw RuntimeException("fail")
            }
        }
        advanceUntilIdle()
        verify(mockTracker).trackEvent(
            any(),
            argThat { containsKey(Params.DURATION_MS) && this[Params.SUCCESS] == "false" }
        )
    }

    @Test
    fun `trackAction with extra params passes them`() = runTest {
        val vm = TestViewModel(mockTracker)
        val extraParams = mapOf("user_id" to "123", "screen" to "home")
        vm.doTrackAction("update_user", params = extraParams) { "ok" }
        advanceUntilIdle()
        verify(mockTracker).trackEvent(
            any(),
            argThat { this["user_id"] == "123" && this["screen"] == "home" }
        )
    }

    @Test
    fun `trackAction failure with null error message uses Unknown error`() = runTest {
        val vm = TestViewModel(mockTracker)
        assertFailsWith<RuntimeException> {
            vm.doTrackAction("action") { throw RuntimeException(null as String?) }
        }
        advanceUntilIdle()
        verify(mockTracker).trackEvent(
            any(),
            argThat { this[Params.ERROR_MESSAGE] == "Unknown error" }
        )
    }

    @Test
    fun `trackAction without trackPerformance does not include duration`() = runTest {
        val vm = TestViewModel(mockTracker)
        vm.doTrackAction("action", trackPerf = false) { "result" }
        advanceUntilIdle()
        verify(mockTracker).trackEvent(
            any(),
            argThat { !containsKey(Params.DURATION_MS) }
        )
    }
}

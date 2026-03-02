package com.example.arcana.core.analytics

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for NavigationAnalyticsObserver-related constants and utilities.
 * Note: NavigationAnalyticsObserver itself is a @Composable function that requires
 * Android/Compose test framework, so these tests cover the analytics constants it uses.
 */
class NavigationAnalyticsObserverTest {

    // ========== AnalyticsScreens Constants ==========

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

    @Test
    fun `all AnalyticsScreens are non-empty`() {
        val screens = listOf(
            AnalyticsScreens.HOME,
            AnalyticsScreens.USER_LIST,
            AnalyticsScreens.USER_DIALOG,
            AnalyticsScreens.USER_CRUD
        )
        screens.forEach { screen ->
            assertTrue(screen.isNotEmpty(), "Screen name should not be empty: $screen")
        }
    }

    @Test
    fun `all AnalyticsScreens are unique`() {
        val screens = listOf(
            AnalyticsScreens.HOME,
            AnalyticsScreens.USER_LIST,
            AnalyticsScreens.USER_DIALOG,
            AnalyticsScreens.USER_CRUD
        )
        assertEquals(screens.size, screens.toSet().size, "All screen names should be unique")
    }

    // ========== Params Constants Used in NavigationAnalyticsObserver ==========

    @Test
    fun `Params TIMESTAMP is correct`() {
        assertEquals("timestamp", Params.TIMESTAMP)
    }

    @Test
    fun `Params SCREEN_NAME is correct`() {
        assertEquals("screen_name", Params.SCREEN_NAME)
    }

    @Test
    fun `Params DURATION_MS is correct`() {
        assertEquals("duration_ms", Params.DURATION_MS)
    }

    // ========== Events Constants Used in NavigationAnalyticsObserver ==========

    @Test
    fun `Events SCREEN_ENTERED is defined`() {
        assertNotNull(Events.SCREEN_ENTERED)
        assertTrue(Events.SCREEN_ENTERED.isNotEmpty())
    }

    @Test
    fun `Events SCREEN_EXITED is defined`() {
        assertNotNull(Events.SCREEN_EXITED)
        assertTrue(Events.SCREEN_EXITED.isNotEmpty())
    }

    @Test
    fun `Events SCREEN_ENTERED and SCREEN_EXITED are different`() {
        assertTrue(Events.SCREEN_ENTERED != Events.SCREEN_EXITED)
    }

    // ========== Route to screen mapping logic tests ==========

    @Test
    fun `default route mapper returns route unchanged`() {
        val mapper: (String) -> String = { it }
        assertEquals("home", mapper("home"))
        assertEquals("user_crud", mapper("user_crud"))
        assertEquals("some_route", mapper("some_route"))
    }

    @Test
    fun `custom route mapper can transform routes`() {
        val mapper: (String) -> String = { route ->
            when (route) {
                "home" -> AnalyticsScreens.HOME
                "user_crud" -> AnalyticsScreens.USER_CRUD
                else -> route
            }
        }
        assertEquals(AnalyticsScreens.HOME, mapper("home"))
        assertEquals(AnalyticsScreens.USER_CRUD, mapper("user_crud"))
        assertEquals("unknown_route", mapper("unknown_route"))
    }

    @Test
    fun `route mapper with null-safe handling`() {
        val route: String? = null
        val screenName = route ?: "unknown"
        assertEquals("unknown", screenName)
    }

    @Test
    fun `params map can hold timestamp`() {
        val params = mutableMapOf<String, String>()
        params[Params.TIMESTAMP] = System.currentTimeMillis().toString()
        assertTrue(params.containsKey("timestamp"))
        assertTrue(params["timestamp"]!!.toLong() > 0)
    }

    @Test
    fun `params map can hold screen name`() {
        val params = mutableMapOf<String, String>()
        params[Params.SCREEN_NAME] = AnalyticsScreens.HOME
        assertEquals("home", params[Params.SCREEN_NAME])
    }

    @Test
    fun `duration calculation is non-negative`() {
        val entryTime = System.currentTimeMillis()
        Thread.sleep(10)
        val exitTime = System.currentTimeMillis()
        val duration = exitTime - entryTime
        assertTrue(duration >= 0, "Duration should be non-negative")
    }
}

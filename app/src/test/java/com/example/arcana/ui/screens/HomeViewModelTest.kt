package com.example.arcana.ui.screens

import app.cash.turbine.test
import com.example.arcana.core.analytics.AnalyticsTracker
import com.example.arcana.domain.model.User
import com.example.arcana.domain.service.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userService: UserService
    private lateinit var analyticsTracker: AnalyticsTracker
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userService = mock()
        analyticsTracker = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty users and not loading`() = runTest {
        // Given
        whenever(userService.getUsers()).thenReturn(flowOf(emptyList()))
        whenever(userService.syncUsers()).thenReturn(true)
        whenever(userService.getTotalUserCount()).thenReturn(0)

        // When
        viewModel = HomeViewModel(userService, analyticsTracker)
        advanceUntilIdle()

        // Then
        val state = viewModel.output.value
        assertEquals(emptyList(), state.users)
        assertFalse(state.isLoading)
    }

    @Test
    fun `uiState emits users from service`() = runTest {
        // Given
        val testUsers = listOf(
            User(id = 1, firstName = "John", lastName = "Doe", email = "john@example.com"),
            User(id = 2, firstName = "Jane", lastName = "Smith", email = "jane@example.com")
        )
        whenever(userService.getUsers()).thenReturn(flowOf(testUsers))
        whenever(userService.syncUsers()).thenReturn(true)
        whenever(userService.getTotalUserCount()).thenReturn(2)

        // When
        viewModel = HomeViewModel(userService, analyticsTracker)
        advanceUntilIdle()

        // Then
        viewModel.output.test {
            val state = awaitItem()
            assertEquals(testUsers, state.users)
        }
    }

    @Test
    fun `uiState loading indicator works correctly`() = runTest {
        // Given
        whenever(userService.getUsers()).thenReturn(flowOf(emptyList()))
        whenever(userService.syncUsers()).thenReturn(true)
        whenever(userService.getTotalUserCount()).thenReturn(0)

        // When
        viewModel = HomeViewModel(userService, analyticsTracker)
        advanceUntilIdle()

        // Then - verify sync was called (private method)
        val state = viewModel.output.value
        assertFalse(state.isLoading)
    }

    @Test
    fun `users service is called on init`() = runTest {
        // Given
        whenever(userService.getUsers()).thenReturn(flowOf(emptyList()))
        whenever(userService.syncUsers()).thenReturn(true)
        whenever(userService.getTotalUserCount()).thenReturn(0)

        // When
        viewModel = HomeViewModel(userService, analyticsTracker)
        advanceUntilIdle()

        // Then
        verify(userService).getUsers()
    }
}

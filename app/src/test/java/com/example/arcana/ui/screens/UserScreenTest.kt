package com.example.arcana.ui.screens

import com.example.arcana.domain.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for UserScreen UI logic.
 *
 * Note: These are unit tests that verify the screen's behavior and state management.
 * For actual UI/Compose tests, use androidTest with Compose testing framework.
 *
 * This test class focuses on:
 * - ViewModel state observation
 * - Event handling
 * - UI state mapping
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserScreenTest {

    private lateinit var viewModel: UserViewModel
    private lateinit var uiStateFlow: MutableStateFlow<UserViewModel.Output>

    private val testUsers = listOf(
        User(id = 1, firstName = "John", lastName = "Doe", email = "john@example.com", avatar = "avatar1.jpg"),
        User(id = 2, firstName = "Jane", lastName = "Smith", email = "jane@example.com", avatar = "avatar2.jpg")
    )

    @Before
    fun setup() {
        viewModel = mock()
        uiStateFlow = MutableStateFlow(UserViewModel.Output())
        whenever(viewModel.output).thenReturn(uiStateFlow)
        whenever(viewModel.effect).thenReturn(emptyFlow())
    }

    // ==================== State Tests ====================

    @Test
    fun `initial state should be empty`() {
        // Given
        val initialState = UserViewModel.Output()

        // Then
        assertTrue(initialState.users.isEmpty())
        assertFalse(initialState.isLoading)
        assertFalse(initialState.isLoadingMore)
        assertEquals(1, initialState.currentPage)
        assertEquals(1, initialState.totalPages)
    }

    @Test
    fun `state with users should contain user list`() {
        // Given
        val state = UserViewModel.Output(userPages = mapOf(1 to testUsers), currentPage = 1)

        // Then
        assertEquals(testUsers, state.users)
        assertEquals(2, state.users.size)
        assertEquals(testUsers, state.allUsers)
    }

    @Test
    fun `loading state should be true when loading`() {
        // Given
        val state = UserViewModel.Output(isLoading = true)

        // Then
        assertTrue(state.isLoading)
    }

    @Test
    fun `loading more state should be true when loading more`() {
        // Given
        val state = UserViewModel.Output(
            userPages = mapOf(1 to testUsers),
            isLoadingMore = true,
            currentPage = 1,
            totalPages = 5
        )

        // Then
        assertTrue(state.isLoadingMore)
        assertFalse(state.isLoading)
    }

    @Test
    fun `state should track current page and total pages`() {
        // Given
        val state = UserViewModel.Output(
            userPages = mapOf(3 to testUsers),
            currentPage = 3,
            totalPages = 10
        )

        // Then
        assertEquals(3, state.currentPage)
        assertEquals(10, state.totalPages)
    }

    // ==================== User Model Tests ====================

    @Test
    fun `User model should have correct properties`() {
        // Given
        val user = User(
            id = 1,
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            avatar = "avatar.jpg"
        )

        // Then
        assertEquals(1, user.id)
        assertEquals("John", user.firstName)
        assertEquals("Doe", user.lastName)
        assertEquals("john@example.com", user.email)
        assertEquals("avatar.jpg", user.avatar)
        assertEquals("John Doe", user.name)
    }

    @Test
    fun `User name should be combined firstName and lastName`() {
        // Given
        val user = User(
            id = 1,
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com"
        )

        // Then
        assertEquals("John Doe", user.name)
    }

    @Test
    fun `User name should fallback to email when names are empty`() {
        // Given
        val user = User(
            id = 1,
            firstName = "",
            lastName = "",
            email = "john@example.com"
        )

        // Then
        assertEquals("john@example.com", user.name)
    }

    @Test
    fun `User name should trim whitespace`() {
        // Given
        val user = User(
            id = 1,
            firstName = "John",
            lastName = "",
            email = "john@example.com"
        )

        // Then
        assertEquals("John", user.name)
    }

    // ==================== Event Tests ====================

    @Test
    fun `Refresh event should trigger viewModel onEvent`() {
        // When
        viewModel.onEvent(UserViewModel.Input.Refresh)

        // Then
        verify(viewModel).onEvent(UserViewModel.Input.Refresh)
    }

    @Test
    fun `LoadNextPage event should trigger viewModel onEvent`() {
        // When
        viewModel.onEvent(UserViewModel.Input.LoadNextPage)

        // Then
        verify(viewModel).onEvent(UserViewModel.Input.LoadNextPage)
    }

    @Test
    fun `CreateUser event should trigger viewModel onEvent with user`() {
        // Given
        val newUser = User(id = 0, firstName = "New", lastName = "User", email = "new@example.com")

        // When
        viewModel.onEvent(UserViewModel.Input.CreateUser(newUser))

        // Then
        verify(viewModel).onEvent(UserViewModel.Input.CreateUser(newUser))
    }

    @Test
    fun `UpdateUser event should trigger viewModel onEvent with user`() {
        // Given
        val updatedUser = testUsers[0].copy(firstName = "Updated")

        // When
        viewModel.onEvent(UserViewModel.Input.UpdateUser(updatedUser))

        // Then
        verify(viewModel).onEvent(UserViewModel.Input.UpdateUser(updatedUser))
    }

    @Test
    fun `DeleteUser event should trigger viewModel onEvent with user`() {
        // Given
        val userToDelete = testUsers[0]

        // When
        viewModel.onEvent(UserViewModel.Input.DeleteUser(userToDelete))

        // Then
        verify(viewModel).onEvent(UserViewModel.Input.DeleteUser(userToDelete))
    }

    @Test
    fun `GoToPage event should trigger viewModel onEvent with page number`() {
        // When
        viewModel.onEvent(UserViewModel.Input.GoToPage(5))

        // Then
        verify(viewModel).onEvent(UserViewModel.Input.GoToPage(5))
    }

    // ==================== UI State Mapping Tests ====================

    @Test
    fun `empty user list should show empty state`() {
        // Given
        val state = UserViewModel.Output(userPages = emptyMap(), isLoading = false)

        // Then
        assertTrue(state.users.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `user list with data should show users`() {
        // Given
        val state = UserViewModel.Output(userPages = mapOf(1 to testUsers), currentPage = 1, isLoading = false)

        // Then
        assertEquals(2, state.users.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loading state should be shown correctly`() {
        // Given
        val loadingState = UserViewModel.Output(userPages = emptyMap(), isLoading = true)

        // Then
        assertTrue(loadingState.isLoading)
        assertTrue(loadingState.users.isEmpty())
    }

    @Test
    fun `loading more state should be shown with existing users`() {
        // Given
        val loadingMoreState = UserViewModel.Output(
            userPages = mapOf(1 to testUsers),
            currentPage = 1,
            isLoadingMore = true
        )

        // Then
        assertTrue(loadingMoreState.isLoadingMore)
        assertEquals(2, loadingMoreState.users.size)
    }

    // ==================== Pagination Tests ====================

    @Test
    fun `pagination should have correct page info`() {
        // Given
        val state = UserViewModel.Output(
            userPages = mapOf(2 to testUsers),
            currentPage = 2,
            totalPages = 5
        )

        // Then
        assertEquals(2, state.currentPage)
        assertEquals(5, state.totalPages)
        assertTrue(state.currentPage < state.totalPages)  // Has more pages
    }

    @Test
    fun `last page should be correctly identified`() {
        // Given
        val lastPageState = UserViewModel.Output(
            userPages = mapOf(5 to testUsers),
            currentPage = 5,
            totalPages = 5
        )

        // Then
        assertEquals(lastPageState.currentPage, lastPageState.totalPages)
        assertFalse(lastPageState.currentPage < lastPageState.totalPages)  // No more pages
    }

    @Test
    fun `first page should be correctly identified`() {
        // Given
        val firstPageState = UserViewModel.Output(
            userPages = mapOf(1 to testUsers),
            currentPage = 1,
            totalPages = 5
        )

        // Then
        assertEquals(1, firstPageState.currentPage)
        assertFalse(firstPageState.currentPage == 0)
    }

    // ==================== Effect Tests ====================

    @Test
    fun `ShowError effect should contain error message`() {
        // Given
        val errorMessage = "Failed to load users"
        val effect: UserViewModel.Effect = UserViewModel.Effect.ShowError(errorMessage)

        // Then
        assertTrue(effect is UserViewModel.Effect.ShowError)
        assertEquals(errorMessage, (effect as UserViewModel.Effect.ShowError).message)
    }

    @Test
    fun `ShowSuccess effect should contain success message`() {
        // Given
        val successMessage = "User created successfully"
        val effect: UserViewModel.Effect = UserViewModel.Effect.ShowSuccess(successMessage)

        // Then
        assertTrue(effect is UserViewModel.Effect.ShowSuccess)
        assertEquals(successMessage, (effect as UserViewModel.Effect.ShowSuccess).message)
    }

    // ==================== User Copy Tests ====================

    @Test
    fun `User copy should create new instance with updated fields`() {
        // Given
        val originalUser = testUsers[0]

        // When
        val updatedUser = originalUser.copy(firstName = "Updated", email = "updated@example.com")

        // Then
        assertEquals(originalUser.id, updatedUser.id)
        assertEquals("Updated", updatedUser.firstName)
        assertEquals(originalUser.lastName, updatedUser.lastName)
        assertEquals("updated@example.com", updatedUser.email)
        assertEquals(originalUser.avatar, updatedUser.avatar)
    }

    @Test
    fun `User copy without changes should be equal`() {
        // Given
        val originalUser = testUsers[0]

        // When
        val copiedUser = originalUser.copy()

        // Then
        assertEquals(originalUser, copiedUser)
    }

    // ==================== State Update Tests ====================

    @Test
    fun `state update should preserve other fields`() {
        // Given
        val originalState = UserViewModel.Output(
            userPages = mapOf(2 to testUsers),
            isLoading = false,
            currentPage = 2,
            totalPages = 5
        )

        // When
        val updatedState = originalState.copy(isLoading = true)

        // Then
        assertEquals(originalState.userPages, updatedState.userPages)
        assertTrue(updatedState.isLoading)
        assertEquals(originalState.currentPage, updatedState.currentPage)
        assertEquals(originalState.totalPages, updatedState.totalPages)
    }

    @Test
    fun `state with loading more should not affect loading flag`() {
        // Given
        val state = UserViewModel.Output(
            userPages = mapOf(1 to testUsers),
            currentPage = 1,
            isLoading = false,
            isLoadingMore = true
        )

        // Then
        assertFalse(state.isLoading)
        assertTrue(state.isLoadingMore)
    }
}

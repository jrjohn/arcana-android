package com.example.arcana.domain.service

import com.example.arcana.data.model.User
import com.example.arcana.data.repository.CacheEventBus
import com.example.arcana.domain.repository.DataRepository
import com.example.arcana.sync.Synchronizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserServiceImplTest {

    private lateinit var userService: UserServiceImpl
    private lateinit var dataRepository: DataRepository
    private lateinit var synchronizer: Synchronizer
    private lateinit var cacheEventBus: CacheEventBus

    private val testUsers = listOf(
        User(id = 1, firstName = "John", lastName = "Doe", email = "john@example.com", avatar = "avatar1.jpg"),
        User(id = 2, firstName = "Jane", lastName = "Smith", email = "jane@example.com", avatar = "avatar2.jpg")
    )

    @Before
    fun setup() {
        dataRepository = mock()
        synchronizer = mock()
        cacheEventBus = mock()
        userService = UserServiceImpl(dataRepository, synchronizer, cacheEventBus)
    }

    @Test
    fun `getUsers should return flow from repository`() = runTest {
        // Given
        val usersFlow = flowOf(testUsers)
        whenever(dataRepository.getUsers()).thenReturn(usersFlow)

        // When
        val result = userService.getUsers()

        // Then
        assertEquals(usersFlow, result)
        verify(dataRepository).getUsers()
    }

    @Test
    fun `getUsersPage should return result from repository`() = runTest {
        // Given
        val page = 2
        val totalPages = 5
        val expectedResult = Result.success(Pair(testUsers, totalPages))
        whenever(dataRepository.getUsersPage(page)).thenReturn(expectedResult)

        // When
        val result = userService.getUsersPage(page)

        // Then
        assertEquals(expectedResult, result)
        verify(dataRepository).getUsersPage(page)
    }

    @Test
    fun `getUsersPage with page 1 should return first page`() = runTest {
        // Given
        val page1Users = listOf(testUsers[0])
        val expectedResult = Result.success(Pair(page1Users, 10))
        whenever(dataRepository.getUsersPage(1)).thenReturn(expectedResult)

        // When
        val result = userService.getUsersPage(1)

        // Then
        assertTrue(result.isSuccess)
        val (users, totalPages) = result.getOrThrow()
        assertEquals(page1Users, users)
        assertEquals(10, totalPages)
    }

    @Test
    fun `getUsersPage failure should return error result`() = runTest {
        // Given
        val page = 2
        val error = Exception("Network error")
        val expectedResult = Result.failure<Pair<List<User>, Int>>(error)
        whenever(dataRepository.getUsersPage(page)).thenReturn(expectedResult)

        // When
        val result = userService.getUsersPage(page)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `getTotalUserCount should return count from repository`() = runTest {
        // Given
        val expectedCount = 42
        whenever(dataRepository.getTotalUserCount()).thenReturn(expectedCount)

        // When
        val result = userService.getTotalUserCount()

        // Then
        assertEquals(expectedCount, result)
        verify(dataRepository).getTotalUserCount()
    }

    @Test
    fun `createUser should call repository with user object`() = runTest {
        // Given
        val newUser = User(id = 0, firstName = "New", lastName = "User", email = "new@example.com")
        whenever(dataRepository.createUser(newUser)).thenReturn(true)

        // When
        val result = userService.createUser(newUser)

        // Then
        assertTrue(result)
        verify(dataRepository).createUser(newUser)
    }

    @Test
    fun `createUser failure should return false`() = runTest {
        // Given
        val newUser = User(id = 0, firstName = "New", lastName = "User", email = "new@example.com")
        whenever(dataRepository.createUser(newUser)).thenReturn(false)

        // When
        val result = userService.createUser(newUser)

        // Then
        assertFalse(result)
        verify(dataRepository).createUser(newUser)
    }

    @Test
    fun `updateUser should call repository with user object`() = runTest {
        // Given
        val updatedUser = testUsers[0].copy(firstName = "Updated")
        whenever(dataRepository.updateUser(updatedUser)).thenReturn(true)

        // When
        val result = userService.updateUser(updatedUser)

        // Then
        assertTrue(result)
        verify(dataRepository).updateUser(updatedUser)
    }

    @Test
    fun `updateUser failure should return false`() = runTest {
        // Given
        val updatedUser = testUsers[0].copy(firstName = "Updated")
        whenever(dataRepository.updateUser(updatedUser)).thenReturn(false)

        // When
        val result = userService.updateUser(updatedUser)

        // Then
        assertFalse(result)
        verify(dataRepository).updateUser(updatedUser)
    }

    @Test
    fun `deleteUser should call repository with user id`() = runTest {
        // Given
        val userId = 1
        whenever(dataRepository.deleteUser(userId)).thenReturn(true)

        // When
        val result = userService.deleteUser(userId)

        // Then
        assertTrue(result)
        verify(dataRepository).deleteUser(userId)
    }

    @Test
    fun `deleteUser failure should return false`() = runTest {
        // Given
        val userId = 1
        whenever(dataRepository.deleteUser(userId)).thenReturn(false)

        // When
        val result = userService.deleteUser(userId)

        // Then
        assertFalse(result)
        verify(dataRepository).deleteUser(userId)
    }

    @Test
    fun `syncUsers should call synchronizer sync`() = runTest {
        // Given
        whenever(synchronizer.sync()).thenReturn(true)

        // When
        val result = userService.syncUsers()

        // Then
        assertTrue(result)
        verify(synchronizer).sync()
    }

    @Test
    fun `syncUsers failure should return false`() = runTest {
        // Given
        whenever(synchronizer.sync()).thenReturn(false)

        // When
        val result = userService.syncUsers()

        // Then
        assertFalse(result)
        verify(synchronizer).sync()
    }

    @Test
    fun `createUser with complete user data should succeed`() = runTest {
        // Given
        val completeUser = User(
            id = 0,
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            avatar = "https://example.com/avatar.jpg"
        )
        whenever(dataRepository.createUser(completeUser)).thenReturn(true)

        // When
        val result = userService.createUser(completeUser)

        // Then
        assertTrue(result)
        verify(dataRepository).createUser(completeUser)
    }

    @Test
    fun `updateUser with partial data should succeed`() = runTest {
        // Given
        val partialUser = User(
            id = 1,
            firstName = "John",
            lastName = "",
            email = ""
        )
        whenever(dataRepository.updateUser(partialUser)).thenReturn(true)

        // When
        val result = userService.updateUser(partialUser)

        // Then
        assertTrue(result)
        verify(dataRepository).updateUser(partialUser)
    }
}

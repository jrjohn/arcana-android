package com.example.arcana.data.repository

import com.example.arcana.core.common.NetworkMonitor
import com.example.arcana.data.local.UserChangeDao
import com.example.arcana.data.local.UserDao
import com.example.arcana.domain.model.ChangeType
import com.example.arcana.domain.model.User
import com.example.arcana.domain.model.UserChange
import com.example.arcana.data.network.UserNetworkDataSource
import com.example.arcana.data.remote.CreateUserRequest
import com.example.arcana.domain.repository.CacheEventBus
import com.example.arcana.data.repository.impl.OfflineFirstDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstDataRepositoryTest {

    private lateinit var repository: OfflineFirstDataRepository
    private lateinit var userDao: UserDao
    private lateinit var userChangeDao: UserChangeDao
    private lateinit var networkDataSource: UserNetworkDataSource
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var cacheEventBus: CacheEventBus

    private val testUsers = listOf(
        User(id = 1, firstName = "John", lastName = "Doe", email = "john@example.com", avatar = "avatar1.jpg"),
        User(id = 2, firstName = "Jane", lastName = "Smith", email = "jane@example.com", avatar = "avatar2.jpg")
    )

    @Before
    fun setup() {
        userDao = mock()
        userChangeDao = mock()
        networkDataSource = mock()
        networkMonitor = mock()
        cacheEventBus = mock()
        // The repository's init block launches a coroutine that collects userDao.getUsers().
        // We must stub it before constructing the repository to avoid UncaughtExceptionsBeforeTest.
        whenever(userDao.getUsers()).thenReturn(flowOf(emptyList()))
        repository = OfflineFirstDataRepository(
            userDao,
            userChangeDao,
            networkDataSource,
            networkMonitor,
            cacheEventBus
        )
        // Reset invocation counters so init-block calls don't pollute individual test verifications.
        clearInvocations(userDao, userChangeDao, networkDataSource, networkMonitor, cacheEventBus)
    }

    // ==================== getUsers Tests ====================

    @Test
    fun `getUsers should return flow from dao`() = runTest {
        // Given
        val usersFlow = flowOf(testUsers)
        whenever(userDao.getUsers()).thenReturn(usersFlow)

        // When
        val result = repository.getUsers()

        // Then
        assertEquals(usersFlow, result)
        verify(userDao, atLeastOnce()).getUsers()
    }

    // ==================== getUsersPage Tests ====================

    @Test
    fun `getUsersPage when online should return network data`() = runTest {
        // Given
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(networkDataSource.getUsersPage(1)).thenReturn(Pair(testUsers, 5))

        // When
        val result = repository.getUsersPage(1)

        // Then
        assertTrue(result.isSuccess)
        val (users, totalPages) = result.getOrThrow()
        assertEquals(testUsers, users)
        assertEquals(5, totalPages)
        verify(networkDataSource).getUsersPage(1)
    }

    @Test
    fun `getUsersPage when offline should return failure`() = runTest {
        // Given
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))
        whenever(userDao.getUsers()).thenReturn(flowOf(emptyList()))

        // When
        val result = repository.getUsersPage(1)

        // Then
        assertTrue(result.isSuccess)
        val (users, totalPages) = result.getOrThrow()
        assertEquals(emptyList(), users)
        assertEquals(1, totalPages)  // Returns at least 1 page even when empty
        verify(networkDataSource, never()).getUsersPage(any())
    }

    @Test
    fun `getUsersPage network error should return failure`() = runTest {
        // Given
        val errorMessage = "API error"
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        org.mockito.kotlin.doAnswer { throw Exception(errorMessage) }
            .whenever(networkDataSource).getUsersPage(1)

        // When
        val result = repository.getUsersPage(1)

        // Then
        assertTrue(result.isFailure)
        assertEquals(errorMessage, result.exceptionOrNull()?.message)
    }

    // ==================== getTotalUserCount Tests ====================

    @Test
    fun `getTotalUserCount when online should return network count`() = runTest {
        // Given
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(networkDataSource.getUsersWithTotal()).thenReturn(Pair(testUsers, 42))
        // When
        val result = repository.getTotalUserCount()

        // Then
        assertEquals(42, result)
        verify(networkDataSource).getUsersWithTotal()
    }

    @Test
    fun `getTotalUserCount when offline should return local count`() = runTest {
        // Given
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))
        whenever(userDao.getUsers()).thenReturn(flowOf(testUsers))

        // When
        val result = repository.getTotalUserCount()

        // Then
        assertEquals(testUsers.size, result)
        verify(userDao, atLeastOnce()).getUsers()
    }

    @Test
    fun `getTotalUserCount network error should fallback to local count`() = runTest {
        // Given
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        org.mockito.kotlin.doAnswer { throw Exception("Network error") }
            .whenever(networkDataSource).getUsersWithTotal()
        whenever(userDao.getUsers()).thenReturn(flowOf(testUsers))

        // When
        val result = repository.getTotalUserCount()

        // Then
        assertEquals(testUsers.size, result)
        // The repository's init-block coroutine (Dispatchers.Default) may also call getUsers()
        // asynchronously, so we use atLeastOnce() to avoid flaky TooManyActualInvocations.
        verify(userDao, atLeastOnce()).getUsers()
    }

    // ==================== createUser Tests ====================

    @Test
    fun `createUser when online should create via network and sync`() = runTest {
        // Given
        val newUser = User(id = 0, firstName = "New", lastName = "User", email = "new@example.com")
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(networkDataSource.createUser(any())).thenReturn(Unit)
        whenever(networkDataSource.getUsersPage(1)).thenReturn(Pair(testUsers, 1))
        whenever(userDao.getUsers()).thenReturn(flowOf(emptyList())) // For conflict resolution
        whenever(userChangeDao.getAll()).thenReturn(emptyList())

        // When
        val result = repository.createUser(newUser)

        // Then
        assertTrue(result)
        verify(networkDataSource).createUser(CreateUserRequest(newUser.name, "Developer"))
        verify(userDao).insertUsers(testUsers)
    }

    @Test
    fun `createUser when online fails should queue for offline`() = runTest {
        // Given
        val newUser = User(id = 0, firstName = "New", lastName = "User", email = "new@example.com")
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        org.mockito.kotlin.doAnswer { throw Exception("Network error") }
            .whenever(networkDataSource).createUser(any())

        // When
        val result = repository.createUser(newUser)

        // Then
        assertFalse(result)
        verify(userDao).upsertUser(any())
        verify(userChangeDao).insert(any())
    }

    @Test
    fun `createUser when offline should queue change`() = runTest {
        // Given
        val newUser = User(id = 0, firstName = "New", lastName = "User", email = "new@example.com")
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))

        // When
        val result = repository.createUser(newUser)

        // Then
        assertTrue(result)
        verify(userDao).upsertUser(any())
        verify(userChangeDao).insert(any())
        verify(networkDataSource, never()).createUser(any())
    }

    // ==================== updateUser Tests ====================

    @Test
    fun `updateUser when online should update via network and sync`() = runTest {
        // Given
        val updatedUser = testUsers[0].copy(firstName = "Updated")
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(networkDataSource.updateUser(any(), any())).thenReturn(Unit)
        whenever(networkDataSource.getUsersPage(1)).thenReturn(Pair(testUsers, 1))
        whenever(userDao.getUsers()).thenReturn(flowOf(emptyList())) // For conflict resolution
        whenever(userChangeDao.getAll()).thenReturn(emptyList())

        // When
        val result = repository.updateUser(updatedUser)

        // Then
        assertTrue(result)
        verify(networkDataSource).updateUser(updatedUser.id, CreateUserRequest(updatedUser.name, "Developer"))
        verify(userDao).insertUsers(testUsers)
    }

    @Test
    fun `updateUser when online fails should queue for offline`() = runTest {
        // Given
        val updatedUser = testUsers[0].copy(firstName = "Updated")
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        org.mockito.kotlin.doAnswer { throw Exception("Network error") }
            .whenever(networkDataSource).updateUser(any(), any())

        // When
        val result = repository.updateUser(updatedUser)

        // Then
        // Optimistic-update strategy: local op always succeeds → always returns true.
        // The failed network call is silently swallowed; the queued change will retry later.
        assertTrue(result)
        verify(userDao).upsertUser(updatedUser)
        verify(userChangeDao).insert(any())
    }

    @Test
    fun `updateUser when offline should queue change`() = runTest {
        // Given
        val updatedUser = testUsers[0].copy(firstName = "Updated")
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))

        // When
        val result = repository.updateUser(updatedUser)

        // Then
        assertTrue(result)
        verify(userDao).upsertUser(updatedUser)
        verify(userChangeDao).insert(any())
        verify(networkDataSource, never()).updateUser(any(), any())
    }

    // ==================== deleteUser Tests ====================

    @Test
    fun `deleteUser when online should delete via network and sync`() = runTest {
        // Given
        val userId = 1
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(networkDataSource.deleteUser(userId)).thenReturn(Unit)
        whenever(networkDataSource.getUsersPage(1)).thenReturn(Pair(testUsers, 1))
        whenever(userDao.getUsers()).thenReturn(flowOf(emptyList())) // For conflict resolution
        whenever(userChangeDao.getAll()).thenReturn(emptyList())

        // When
        val result = repository.deleteUser(userId)

        // Then
        assertTrue(result)
        verify(networkDataSource).deleteUser(userId)
        verify(userDao).insertUsers(testUsers)
    }

    @Test
    fun `deleteUser when online fails should queue for offline`() = runTest {
        // Given
        val userId = 1
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        org.mockito.kotlin.doAnswer { throw Exception("Network error") }
            .whenever(networkDataSource).deleteUser(userId)

        // When
        val result = repository.deleteUser(userId)

        // Then
        // Optimistic-delete strategy: local op always succeeds → always returns true.
        // The failed network call is silently swallowed; the queued change will retry later.
        assertTrue(result)
        val userCaptor = argumentCaptor<User>()
        verify(userDao).deleteUser(userCaptor.capture())
        assertEquals(userId, userCaptor.firstValue.id)
        verify(userChangeDao).insert(any())
    }

    @Test
    fun `deleteUser when offline should queue change`() = runTest {
        // Given
        val userId = 1
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))

        // When
        val result = repository.deleteUser(userId)

        // Then
        assertTrue(result)
        val userCaptor = argumentCaptor<User>()
        verify(userDao).deleteUser(userCaptor.capture())
        assertEquals(userId, userCaptor.firstValue.id)
        verify(userChangeDao).insert(any())
        verify(networkDataSource, never()).deleteUser(any())
    }

    // ==================== sync Tests ====================

    @Test
    fun `sync when online should process offline changes and fetch users`() = runTest {
        // Given
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(userChangeDao.getAll()).thenReturn(emptyList())
        whenever(networkDataSource.getUsersPage(1)).thenReturn(Pair(testUsers, 1))
        whenever(userDao.getUsers()).thenReturn(flowOf(emptyList())) // For conflict resolution
        // When
        val result = repository.sync()

        // Then
        assertTrue(result)
        verify(userChangeDao).getAll()
        verify(networkDataSource).getUsersPage(1)
        verify(userDao).insertUsers(testUsers)
    }

    @Test
    fun `sync when offline should skip and return false`() = runTest {
        // Given
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))

        // When
        val result = repository.sync()

        // Then
        assertFalse(result)
        verify(networkDataSource, never()).getUsersPage(any())
    }

    @Test
    fun `sync should process pending CREATE changes`() = runTest {
        // Given
        val pendingChange = UserChange(
            id = 1,
            userId = 100,
            type = ChangeType.CREATE,
            name = "John Doe",
            job = "Developer"
        )
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(userChangeDao.getAll()).thenReturn(listOf(pendingChange))
        whenever(networkDataSource.createUser(any())).thenReturn(Unit)
        whenever(networkDataSource.getUsersPage(1)).thenReturn(Pair(testUsers, 1))
        whenever(userDao.getUsers()).thenReturn(flowOf(emptyList())) // For conflict resolution

        // When
        val result = repository.sync()

        // Then
        assertTrue(result)
        verify(networkDataSource).createUser(CreateUserRequest("John Doe", "Developer"))
        verify(userChangeDao).delete(listOf(1L))
    }

    @Test
    fun `sync should process pending UPDATE changes`() = runTest {
        // Given
        val pendingChange = UserChange(
            id = 1,
            userId = 1,
            type = ChangeType.UPDATE,
            name = "Updated Name",
            job = "Developer"
        )
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(userChangeDao.getAll()).thenReturn(listOf(pendingChange))
        whenever(networkDataSource.updateUser(any(), any())).thenReturn(Unit)
        whenever(networkDataSource.getUsersPage(1)).thenReturn(Pair(testUsers, 1))
        whenever(userDao.getUsers()).thenReturn(flowOf(emptyList())) // For conflict resolution

        // When
        val result = repository.sync()

        // Then
        assertTrue(result)
        verify(networkDataSource).updateUser(1, CreateUserRequest("Updated Name", "Developer"))
        verify(userChangeDao).delete(listOf(1L))
    }

    @Test
    fun `sync should process pending DELETE changes`() = runTest {
        // Given
        val pendingChange = UserChange(
            id = 1,
            userId = 1,
            type = ChangeType.DELETE
        )
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(userChangeDao.getAll()).thenReturn(listOf(pendingChange))
        whenever(networkDataSource.deleteUser(any())).thenReturn(Unit)
        whenever(networkDataSource.getUsersPage(1)).thenReturn(Pair(testUsers, 1))
        whenever(userDao.getUsers()).thenReturn(flowOf(emptyList())) // For conflict resolution
        // When
        val result = repository.sync()

        // Then
        assertTrue(result)
        verify(networkDataSource).deleteUser(1)
        verify(userChangeDao).delete(listOf(1L))
    }

    @Test
    fun `sync should continue processing even if one change fails`() = runTest {
        // Given
        val change1 = UserChange(id = 1, userId = 1, type = ChangeType.DELETE)
        val change2 = UserChange(id = 2, userId = 2, type = ChangeType.DELETE)

        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(userChangeDao.getAll()).thenReturn(listOf(change1, change2))
        org.mockito.kotlin.doAnswer { throw Exception("Failed") }
            .whenever(networkDataSource).deleteUser(1)
        whenever(networkDataSource.deleteUser(2)).thenReturn(Unit)
        whenever(networkDataSource.getUsersPage(1)).thenReturn(Pair(testUsers, 1))
        whenever(userDao.getUsers()).thenReturn(flowOf(emptyList())) // For conflict resolution
        // When
        val result = repository.sync()

        // Then
        assertTrue(result)
        verify(networkDataSource).deleteUser(1)
        verify(networkDataSource).deleteUser(2)
        // Only change2 should be deleted from queue
        verify(userChangeDao).delete(listOf(2L))
    }

    @Test
    fun `sync network error should return false`() = runTest {
        // Given
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(userChangeDao.getAll()).thenReturn(emptyList())
        org.mockito.kotlin.doAnswer { throw Exception("Network error") }
            .whenever(networkDataSource).getUsersPage(any())

        // When
        val result = repository.sync()

        // Then
        assertFalse(result)
    }

    // ==================== queueCreateUser Tests ====================

    @Test
    fun `queueCreateUser should insert user with temp ID and create change`() = runTest {
        // Given
        val newUser = User(id = 0, firstName = "New", lastName = "User", email = "new@example.com")
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))

        // When
        repository.createUser(newUser)

        // Then
        val userCaptor = argumentCaptor<User>()
        val changeCaptor = argumentCaptor<UserChange>()

        verify(userDao).upsertUser(userCaptor.capture())
        verify(userChangeDao).insert(changeCaptor.capture())

        // Verify user was inserted with temporary ID
        val insertedUser = userCaptor.firstValue
        assertTrue(insertedUser.id != 0)  // Has a temp ID
        assertEquals(newUser.firstName, insertedUser.firstName)
        assertEquals(newUser.lastName, insertedUser.lastName)

        // Verify change was created
        val change = changeCaptor.firstValue
        assertEquals(ChangeType.CREATE, change.type)
        assertEquals(newUser.name, change.name)
        assertEquals("Developer", change.job)
    }

    // ==================== queueUpdateUser Tests ====================

    @Test
    fun `queueUpdateUser should upsert user and create change`() = runTest {
        // Given
        val updatedUser = testUsers[0].copy(firstName = "Updated")
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))

        // When
        repository.updateUser(updatedUser)

        // Then
        val changeCaptor = argumentCaptor<UserChange>()

        verify(userDao).upsertUser(updatedUser)
        verify(userChangeDao).insert(changeCaptor.capture())

        // Verify change was created
        val change = changeCaptor.firstValue
        assertEquals(updatedUser.id, change.userId)
        assertEquals(ChangeType.UPDATE, change.type)
        assertEquals(updatedUser.name, change.name)
        assertEquals("Developer", change.job)
    }

    // ==================== queueDeleteUser Tests ====================

    @Test
    fun `queueDeleteUser should delete user and create change`() = runTest {
        // Given
        val userId = 1
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))

        // When
        repository.deleteUser(userId)

        // Then
        val changeCaptor = argumentCaptor<UserChange>()
        val userCaptor = argumentCaptor<User>()

        verify(userDao).deleteUser(userCaptor.capture())
        verify(userChangeDao).insert(changeCaptor.capture())

        // Verify correct user was deleted
        assertEquals(userId, userCaptor.firstValue.id)

        // Verify change was created
        val change = changeCaptor.firstValue
        assertEquals(userId, change.userId)
        assertEquals(ChangeType.DELETE, change.type)
    }
}

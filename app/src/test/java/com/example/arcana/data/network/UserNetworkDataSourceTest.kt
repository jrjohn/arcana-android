package com.example.arcana.data.network

import com.example.arcana.data.remote.ApiService
import com.example.arcana.data.remote.CreateUserRequest
import com.example.arcana.data.remote.CreateUserResponse
import com.example.arcana.data.remote.UserDto
import com.example.arcana.data.remote.UsersResponse
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.doThrow
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for UserNetworkDataSource.
 * All branches: success, exception, mapping correctness.
 */
class UserNetworkDataSourceTest {

    private val mockApiService: ApiService = mock()
    private val dataSource = UserNetworkDataSource(mockApiService)

    // ─── helpers ──────────────────────────────────────────────────────────────

    private fun dto(id: Int = 1, email: String = "u$id@example.com",
                    first: String = "First$id", last: String = "Last$id",
                    avatar: String = "avatar$id.jpg") =
        UserDto(id, email, first, last, avatar)

    private fun response(vararg dtos: UserDto, page: Int = 1, total: Int = 12, totalPages: Int = 2) =
        UsersResponse(page = page, perPage = 6, total = total, totalPages = totalPages,
            data = dtos.toList())

    // ─── getUsers ─────────────────────────────────────────────────────────────

    @Test
    fun `getUsers - success - returns mapped user list`() = runTest {
        whenever(mockApiService.getUsers()).thenReturn(response(dto(1), dto(2)))
        val result = dataSource.getUsers()
        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals(2, result[1].id)
    }

    @Test
    fun `getUsers - success - maps all UserDto fields`() = runTest {
        val d = dto(42, "alice@example.com", "Alice", "Wonderland", "https://cdn.example.com/alice.png")
        whenever(mockApiService.getUsers()).thenReturn(response(d))
        val user = dataSource.getUsers().single()
        assertEquals(42, user.id)
        assertEquals("alice@example.com", user.email)
        assertEquals("Alice", user.firstName)
        assertEquals("Wonderland", user.lastName)
        assertEquals("https://cdn.example.com/alice.png", user.avatar)
    }

    @Test
    fun `getUsers - success - empty list`() = runTest {
        whenever(mockApiService.getUsers()).thenReturn(response())
        val result = dataSource.getUsers()
        assertEquals(0, result.size)
    }

    @Test
    fun `getUsers - network exception - propagates`() = runTest {
        whenever(mockApiService.getUsers()).thenThrow(RuntimeException("network error"))
        var caught: Throwable? = null
        try { dataSource.getUsers() } catch (e: Exception) { caught = e }
        assertEquals("network error", caught?.message)
    }

    @Test
    fun `getUsers - verifies apiService is called`() = runTest {
        whenever(mockApiService.getUsers()).thenReturn(response())
        dataSource.getUsers()
        verify(mockApiService).getUsers()
    }

    // ─── getUsersWithTotal ────────────────────────────────────────────────────

    @Test
    fun `getUsersWithTotal - success - returns pair with total`() = runTest {
        whenever(mockApiService.getUsers()).thenReturn(response(dto(1), dto(2), total = 99))
        val (users, total) = dataSource.getUsersWithTotal()
        assertEquals(2, users.size)
        assertEquals(99, total)
    }

    @Test
    fun `getUsersWithTotal - success - maps users correctly`() = runTest {
        val d = dto(7, "seven@example.com", "Seven", "Stars", "star.jpg")
        whenever(mockApiService.getUsers()).thenReturn(response(d, total = 1, totalPages = 1))
        val (users, _) = dataSource.getUsersWithTotal()
        assertEquals("seven@example.com", users.single().email)
    }

    @Test
    fun `getUsersWithTotal - exception - propagates`() = runTest {
        whenever(mockApiService.getUsers()).thenThrow(RuntimeException("connection reset"))
        var caught: Throwable? = null
        try { dataSource.getUsersWithTotal() } catch (e: Exception) { caught = e }
        assertEquals("connection reset", caught?.message)
    }

    // ─── getUsersPage ─────────────────────────────────────────────────────────

    @Test
    fun `getUsersPage - success - returns pair with totalPages`() = runTest {
        whenever(mockApiService.getUsersPage(2)).thenReturn(response(dto(3), dto(4), page = 2, totalPages = 5))
        val (users, totalPages) = dataSource.getUsersPage(2)
        assertEquals(2, users.size)
        assertEquals(5, totalPages)
    }

    @Test
    fun `getUsersPage - success - passes correct page number`() = runTest {
        whenever(mockApiService.getUsersPage(3)).thenReturn(response(dto(5), page = 3, totalPages = 3))
        dataSource.getUsersPage(3)
        verify(mockApiService).getUsersPage(3)
    }

    @Test
    fun `getUsersPage - success - maps user fields`() = runTest {
        val d = dto(10, "ten@example.com", "Ten", "Eleven", "ten.jpg")
        whenever(mockApiService.getUsersPage(1)).thenReturn(response(d, totalPages = 1))
        val (users, _) = dataSource.getUsersPage(1)
        assertEquals("ten@example.com", users.single().email)
    }

    @Test
    fun `getUsersPage - exception - propagates`() = runTest {
        doThrow(RuntimeException("timeout")).whenever(mockApiService).getUsersPage(any())
        var caught: Throwable? = null
        try { dataSource.getUsersPage(1) } catch (e: Exception) { caught = e }
        assertEquals("timeout", caught?.message)
    }

    // ─── createUser ───────────────────────────────────────────────────────────

    @Test
    fun `createUser - success - completes without error`() = runTest {
        val request = CreateUserRequest("Alice", "Engineer")
        whenever(mockApiService.createUser(request)).thenReturn(
            CreateUserResponse("Alice", "Engineer", id = "123", createdAt = "2024-01-01T00:00:00Z")
        )
        dataSource.createUser(request)   // should not throw
        verify(mockApiService).createUser(request)
    }

    @Test
    fun `createUser - verifies name and job forwarded`() = runTest {
        val request = CreateUserRequest("Bob", "Manager")
        whenever(mockApiService.createUser(request)).thenReturn(
            CreateUserResponse("Bob", "Manager", id = "456", createdAt = "2024-01-02T00:00:00Z")
        )
        dataSource.createUser(request)
        verify(mockApiService).createUser(CreateUserRequest("Bob", "Manager"))
    }

    @Test
    fun `createUser - exception - propagates`() = runTest {
        whenever(mockApiService.createUser(any())).thenThrow(RuntimeException("server error"))
        var caught: Throwable? = null
        try { dataSource.createUser(CreateUserRequest("X", "Y")) } catch (e: Exception) { caught = e }
        assertEquals("server error", caught?.message)
    }

    // ─── updateUser ───────────────────────────────────────────────────────────

    @Test
    fun `updateUser - success - completes without error`() = runTest {
        val request = CreateUserRequest("Updated", "Senior Manager")
        whenever(mockApiService.updateUser(1, request)).thenReturn(
            CreateUserResponse("Updated", "Senior Manager", updatedAt = "2024-01-03T00:00:00Z")
        )
        dataSource.updateUser(1, request)
        verify(mockApiService).updateUser(1, request)
    }

    @Test
    fun `updateUser - verifies correct id forwarded`() = runTest {
        val request = CreateUserRequest("Carol", "Director")
        whenever(mockApiService.updateUser(42, request)).thenReturn(
            CreateUserResponse("Carol", "Director", updatedAt = "2024-01-04T00:00:00Z")
        )
        dataSource.updateUser(42, request)
        verify(mockApiService).updateUser(42, request)
    }

    @Test
    fun `updateUser - exception - propagates`() = runTest {
        whenever(mockApiService.updateUser(any(), any())).thenThrow(RuntimeException("not found"))
        var caught: Throwable? = null
        try { dataSource.updateUser(99, CreateUserRequest("Z", "Z")) } catch (e: Exception) { caught = e }
        assertEquals("not found", caught?.message)
    }

    // ─── deleteUser ───────────────────────────────────────────────────────────

    @Test
    fun `deleteUser - success - completes without error`() = runTest {
        whenever(mockApiService.deleteUser(1)).thenReturn(Unit)
        dataSource.deleteUser(1)
        verify(mockApiService).deleteUser(1)
    }

    @Test
    fun `deleteUser - verifies correct id forwarded`() = runTest {
        whenever(mockApiService.deleteUser(55)).thenReturn(Unit)
        dataSource.deleteUser(55)
        verify(mockApiService).deleteUser(55)
    }

    @Test
    fun `deleteUser - exception - propagates`() = runTest {
        whenever(mockApiService.deleteUser(any())).thenThrow(RuntimeException("forbidden"))
        var caught: Throwable? = null
        try { dataSource.deleteUser(999) } catch (e: Exception) { caught = e }
        assertEquals("forbidden", caught?.message)
    }

    // ─── toUser mapping edge cases ─────────────────────────────────────────────

    @Test
    fun `getUsers - multiple users preserve order`() = runTest {
        val dtos = (1..5).map { dto(it) }
        whenever(mockApiService.getUsers()).thenReturn(
            UsersResponse(1, 6, 5, 1, dtos)
        )
        val result = dataSource.getUsers()
        assertEquals(listOf(1, 2, 3, 4, 5), result.map { it.id })
    }

    @Test
    fun `getUsersWithTotal - empty data - returns zero total`() = runTest {
        whenever(mockApiService.getUsers()).thenReturn(
            UsersResponse(1, 6, 0, 0, emptyList())
        )
        val (users, total) = dataSource.getUsersWithTotal()
        assertEquals(0, users.size)
        assertEquals(0, total)
    }
}

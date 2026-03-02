package com.example.arcana.data.remote

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for data classes defined in ApiService.kt:
 * UserDto, UsersResponse, CreateUserRequest, CreateUserResponse.
 * These are pure-Kotlin serializable data classes with no Android dependencies.
 */
class ApiServiceDataClassTest {

    // ===================== UserDto =====================

    private fun makeUserDto(
        id: Int = 1,
        email: String = "test@example.com",
        firstName: String = "John",
        lastName: String = "Doe",
        avatar: String = "https://example.com/avatar.jpg"
    ) = UserDto(id, email, firstName, lastName, avatar)

    @Test
    fun `UserDto can be instantiated`() {
        val dto = makeUserDto()
        assertNotNull(dto)
    }

    @Test
    fun `UserDto fields are accessible`() {
        val dto = makeUserDto(
            id = 42,
            email = "alice@example.com",
            firstName = "Alice",
            lastName = "Smith",
            avatar = "https://cdn.example.com/alice.png"
        )
        assertEquals(42, dto.id)
        assertEquals("alice@example.com", dto.email)
        assertEquals("Alice", dto.firstName)
        assertEquals("Smith", dto.lastName)
        assertEquals("https://cdn.example.com/alice.png", dto.avatar)
    }

    @Test
    fun `UserDto equals works for identical objects`() {
        val dto1 = makeUserDto()
        val dto2 = makeUserDto()
        assertEquals(dto1, dto2)
    }

    @Test
    fun `UserDto not equals for different id`() {
        val dto1 = makeUserDto(id = 1)
        val dto2 = makeUserDto(id = 2)
        assertNotEquals(dto1, dto2)
    }

    @Test
    fun `UserDto not equals for different email`() {
        val dto1 = makeUserDto(email = "a@example.com")
        val dto2 = makeUserDto(email = "b@example.com")
        assertNotEquals(dto1, dto2)
    }

    @Test
    fun `UserDto hashCode is consistent`() {
        val dto1 = makeUserDto()
        val dto2 = makeUserDto()
        assertEquals(dto1.hashCode(), dto2.hashCode())
    }

    @Test
    fun `UserDto toString contains class name`() {
        val dto = makeUserDto()
        assertTrue(dto.toString().contains("UserDto"))
    }

    @Test
    fun `UserDto copy changes firstName`() {
        val original = makeUserDto()
        val copy = original.copy(firstName = "Jane")
        assertEquals("Jane", copy.firstName)
        assertEquals(original.id, copy.id)
    }

    @Test
    fun `UserDto copy changes lastName`() {
        val original = makeUserDto(lastName = "Doe")
        val copy = original.copy(lastName = "Smith")
        assertEquals("Smith", copy.lastName)
    }

    @Test
    fun `UserDto copy changes email`() {
        val original = makeUserDto(email = "old@example.com")
        val copy = original.copy(email = "new@example.com")
        assertEquals("new@example.com", copy.email)
    }

    @Test
    fun `UserDto copy changes avatar`() {
        val original = makeUserDto(avatar = "old.png")
        val copy = original.copy(avatar = "new.png")
        assertEquals("new.png", copy.avatar)
    }

    @Test
    fun `UserDto with empty strings`() {
        val dto = makeUserDto(email = "", firstName = "", lastName = "", avatar = "")
        assertEquals("", dto.email)
        assertEquals("", dto.firstName)
    }

    @Test
    fun `UserDto component functions work`() {
        val dto = makeUserDto(id = 10, email = "test@example.com")
        val (id, email, firstName, lastName, avatar) = dto
        assertEquals(10, id)
        assertEquals("test@example.com", email)
        assertNotNull(firstName)
        assertNotNull(lastName)
        assertNotNull(avatar)
    }

    // ===================== UsersResponse =====================

    private fun makeUsersResponse(
        page: Int = 1,
        perPage: Int = 6,
        total: Int = 12,
        totalPages: Int = 2,
        data: List<UserDto> = emptyList()
    ) = UsersResponse(page, perPage, total, totalPages, data)

    @Test
    fun `UsersResponse can be instantiated`() {
        val response = makeUsersResponse()
        assertNotNull(response)
    }

    @Test
    fun `UsersResponse fields are accessible`() {
        val users = listOf(makeUserDto(id = 1), makeUserDto(id = 2))
        val response = makeUsersResponse(
            page = 2,
            perPage = 10,
            total = 20,
            totalPages = 2,
            data = users
        )
        assertEquals(2, response.page)
        assertEquals(10, response.perPage)
        assertEquals(20, response.total)
        assertEquals(2, response.totalPages)
        assertEquals(2, response.data.size)
    }

    @Test
    fun `UsersResponse equals works`() {
        val r1 = makeUsersResponse(page = 1, total = 5)
        val r2 = makeUsersResponse(page = 1, total = 5)
        assertEquals(r1, r2)
    }

    @Test
    fun `UsersResponse not equals for different page`() {
        val r1 = makeUsersResponse(page = 1)
        val r2 = makeUsersResponse(page = 2)
        assertNotEquals(r1, r2)
    }

    @Test
    fun `UsersResponse hashCode consistent`() {
        val r1 = makeUsersResponse()
        val r2 = makeUsersResponse()
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `UsersResponse toString contains class name`() {
        val r = makeUsersResponse()
        assertTrue(r.toString().contains("UsersResponse"))
    }

    @Test
    fun `UsersResponse copy changes page`() {
        val original = makeUsersResponse(page = 1)
        val copy = original.copy(page = 3)
        assertEquals(3, copy.page)
        assertEquals(original.total, copy.total)
    }

    @Test
    fun `UsersResponse copy changes data`() {
        val original = makeUsersResponse(data = emptyList())
        val users = listOf(makeUserDto())
        val copy = original.copy(data = users)
        assertEquals(1, copy.data.size)
    }

    @Test
    fun `UsersResponse with empty data list`() {
        val response = makeUsersResponse(data = emptyList())
        assertTrue(response.data.isEmpty())
    }

    @Test
    fun `UsersResponse with multiple users`() {
        val users = (1..5).map { makeUserDto(id = it) }
        val response = makeUsersResponse(data = users, total = 5)
        assertEquals(5, response.data.size)
        assertEquals(5, response.total)
    }

    @Test
    fun `UsersResponse component functions work`() {
        val response = makeUsersResponse(page = 1, perPage = 6, total = 12, totalPages = 2)
        val (page, perPage, total, totalPages, data) = response
        assertEquals(1, page)
        assertEquals(6, perPage)
        assertEquals(12, total)
        assertEquals(2, totalPages)
        assertNotNull(data)
    }

    // ===================== CreateUserRequest =====================

    private fun makeCreateUserRequest(
        name: String = "John Doe",
        job: String = "Engineer"
    ) = CreateUserRequest(name, job)

    @Test
    fun `CreateUserRequest can be instantiated`() {
        val request = makeCreateUserRequest()
        assertNotNull(request)
    }

    @Test
    fun `CreateUserRequest fields are accessible`() {
        val request = makeCreateUserRequest(name = "Alice", job = "Designer")
        assertEquals("Alice", request.name)
        assertEquals("Designer", request.job)
    }

    @Test
    fun `CreateUserRequest equals works`() {
        val r1 = makeCreateUserRequest()
        val r2 = makeCreateUserRequest()
        assertEquals(r1, r2)
    }

    @Test
    fun `CreateUserRequest not equals for different name`() {
        val r1 = makeCreateUserRequest(name = "Alice")
        val r2 = makeCreateUserRequest(name = "Bob")
        assertNotEquals(r1, r2)
    }

    @Test
    fun `CreateUserRequest not equals for different job`() {
        val r1 = makeCreateUserRequest(job = "Engineer")
        val r2 = makeCreateUserRequest(job = "Designer")
        assertNotEquals(r1, r2)
    }

    @Test
    fun `CreateUserRequest hashCode consistent`() {
        val r1 = makeCreateUserRequest()
        val r2 = makeCreateUserRequest()
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `CreateUserRequest toString contains class name`() {
        val request = makeCreateUserRequest()
        assertTrue(request.toString().contains("CreateUserRequest"))
    }

    @Test
    fun `CreateUserRequest copy changes name`() {
        val original = makeCreateUserRequest(name = "Alice")
        val copy = original.copy(name = "Bob")
        assertEquals("Bob", copy.name)
        assertEquals(original.job, copy.job)
    }

    @Test
    fun `CreateUserRequest copy changes job`() {
        val original = makeCreateUserRequest(job = "Engineer")
        val copy = original.copy(job = "Manager")
        assertEquals("Manager", copy.job)
    }

    @Test
    fun `CreateUserRequest component functions work`() {
        val request = makeCreateUserRequest(name = "Alice", job = "QA")
        val (name, job) = request
        assertEquals("Alice", name)
        assertEquals("QA", job)
    }

    @Test
    fun `CreateUserRequest with empty strings`() {
        val request = makeCreateUserRequest(name = "", job = "")
        assertEquals("", request.name)
        assertEquals("", request.job)
    }

    // ===================== CreateUserResponse =====================

    private fun makeCreateUserResponse(
        name: String = "John Doe",
        job: String = "Engineer",
        id: String? = "12345",
        createdAt: String? = "2024-01-01T00:00:00Z",
        updatedAt: String? = null
    ) = CreateUserResponse(name, job, id, createdAt, updatedAt)

    @Test
    fun `CreateUserResponse can be instantiated`() {
        val response = makeCreateUserResponse()
        assertNotNull(response)
    }

    @Test
    fun `CreateUserResponse fields are accessible`() {
        val response = makeCreateUserResponse(
            name = "Alice",
            job = "Designer",
            id = "42",
            createdAt = "2024-01-15T10:00:00Z",
            updatedAt = "2024-01-15T10:00:00Z"
        )
        assertEquals("Alice", response.name)
        assertEquals("Designer", response.job)
        assertEquals("42", response.id)
        assertEquals("2024-01-15T10:00:00Z", response.createdAt)
        assertEquals("2024-01-15T10:00:00Z", response.updatedAt)
    }

    @Test
    fun `CreateUserResponse id defaults to null`() {
        val response = CreateUserResponse(name = "Alice", job = "Dev")
        assertNull(response.id)
    }

    @Test
    fun `CreateUserResponse createdAt defaults to null`() {
        val response = CreateUserResponse(name = "Alice", job = "Dev")
        assertNull(response.createdAt)
    }

    @Test
    fun `CreateUserResponse updatedAt defaults to null`() {
        val response = CreateUserResponse(name = "Alice", job = "Dev")
        assertNull(response.updatedAt)
    }

    @Test
    fun `CreateUserResponse equals works`() {
        val r1 = makeCreateUserResponse()
        val r2 = makeCreateUserResponse()
        assertEquals(r1, r2)
    }

    @Test
    fun `CreateUserResponse not equals for different name`() {
        val r1 = makeCreateUserResponse(name = "Alice")
        val r2 = makeCreateUserResponse(name = "Bob")
        assertNotEquals(r1, r2)
    }

    @Test
    fun `CreateUserResponse hashCode consistent`() {
        val r1 = makeCreateUserResponse()
        val r2 = makeCreateUserResponse()
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `CreateUserResponse toString contains class name`() {
        val response = makeCreateUserResponse()
        assertTrue(response.toString().contains("CreateUserResponse"))
    }

    @Test
    fun `CreateUserResponse copy changes id`() {
        val original = makeCreateUserResponse(id = "old-id")
        val copy = original.copy(id = "new-id")
        assertEquals("new-id", copy.id)
        assertEquals(original.name, copy.name)
    }

    @Test
    fun `CreateUserResponse copy changes updatedAt`() {
        val original = makeCreateUserResponse(updatedAt = null)
        val copy = original.copy(updatedAt = "2024-02-01T00:00:00Z")
        assertEquals("2024-02-01T00:00:00Z", copy.updatedAt)
    }

    @Test
    fun `CreateUserResponse copy keeps nulls`() {
        val original = CreateUserResponse(name = "Test", job = "Dev")
        val copy = original.copy(name = "Changed")
        assertEquals("Changed", copy.name)
        assertNull(copy.id)
        assertNull(copy.createdAt)
    }

    @Test
    fun `CreateUserResponse component functions work`() {
        val response = makeCreateUserResponse(name = "Alice", job = "PM", id = "99")
        val (name, job, id, createdAt, updatedAt) = response
        assertEquals("Alice", name)
        assertEquals("PM", job)
        assertEquals("99", id)
    }

    @Test
    fun `CreateUserResponse not equals for different id`() {
        val r1 = makeCreateUserResponse(id = "1")
        val r2 = makeCreateUserResponse(id = "2")
        assertNotEquals(r1, r2)
    }
}

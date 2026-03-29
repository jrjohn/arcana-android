package com.example.arcana.domain.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for User data class including the computed `name` property.
 */
class UserTest {

    private fun makeUser(
        id: Int = 1,
        email: String = "john@example.com",
        firstName: String = "John",
        lastName: String = "Doe",
        avatar: String = "https://example.com/avatar.jpg",
        version: Int = 1
    ) = User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        avatar = avatar,
        version = version
    )

    // ===================== Instantiation =====================

    @Test
    fun `User can be instantiated`() {
        val user = makeUser()
        assertNotNull(user)
    }

    @Test
    fun `User fields are accessible`() {
        val user = makeUser(
            id = 42,
            email = "alice@example.com",
            firstName = "Alice",
            lastName = "Smith",
            avatar = "https://cdn.example.com/alice.jpg"
        )
        assertEquals(42, user.id)
        assertEquals("alice@example.com", user.email)
        assertEquals("Alice", user.firstName)
        assertEquals("Smith", user.lastName)
        assertEquals("https://cdn.example.com/alice.jpg", user.avatar)
    }

    // ===================== name computed property =====================

    @Test
    fun `name returns full name when both first and last are non-empty`() {
        val user = makeUser(firstName = "John", lastName = "Doe")
        assertEquals("John Doe", user.name)
    }

    @Test
    fun `name returns firstName only when lastName is empty`() {
        val user = makeUser(firstName = "Alice", lastName = "")
        assertEquals("Alice", user.name)
    }

    @Test
    fun `name returns lastName only when firstName is empty`() {
        val user = makeUser(firstName = "", lastName = "Smith")
        assertEquals("Smith", user.name)
    }

    @Test
    fun `name falls back to email when both names are empty`() {
        val user = makeUser(firstName = "", lastName = "", email = "fallback@example.com")
        assertEquals("fallback@example.com", user.name)
    }

    @Test
    fun `name falls back to email when both names are blank spaces`() {
        val user = makeUser(firstName = " ", lastName = " ", email = "blank@example.com")
        // trim() should remove surrounding spaces; " ".trim() is "" so ifEmpty returns email
        assertEquals("blank@example.com", user.name)
    }

    @Test
    fun `name returns non-empty concatenation for typical user`() {
        val user = makeUser(firstName = "Jane", lastName = "Doe")
        assertTrue(user.name.isNotEmpty())
    }

    @Test
    fun `name is Jane Doe for firstName Jane lastName Doe`() {
        val user = makeUser(firstName = "Jane", lastName = "Doe")
        assertEquals("Jane Doe", user.name)
    }

    // ===================== equals & hashCode =====================

    @Test
    fun `User equals works for identical objects`() {
        val user1 = makeUser()
        val user2 = makeUser()
        assertEquals(user1, user2)
    }

    @Test
    fun `User not equals for different id`() {
        val user1 = makeUser(id = 1)
        val user2 = makeUser(id = 2)
        assertNotEquals(user1, user2)
    }

    @Test
    fun `User not equals for different email`() {
        val user1 = makeUser(email = "a@example.com")
        val user2 = makeUser(email = "b@example.com")
        assertNotEquals(user1, user2)
    }

    @Test
    fun `User hashCode is consistent`() {
        val user1 = makeUser()
        val user2 = makeUser()
        assertEquals(user1.hashCode(), user2.hashCode())
    }

    // ===================== copy =====================

    @Test
    fun `User copy changes firstName`() {
        val original = makeUser(firstName = "John")
        val copy = original.copy(firstName = "Jane")
        assertEquals("Jane", copy.firstName)
        assertEquals(original.id, copy.id)
    }

    @Test
    fun `User copy changes email`() {
        val original = makeUser(email = "old@example.com")
        val copy = original.copy(email = "new@example.com")
        assertEquals("new@example.com", copy.email)
    }

    @Test
    fun `User copy changes version`() {
        val original = makeUser(version = 1)
        val copy = original.copy(version = 2)
        assertEquals(2, copy.version)
    }

    // ===================== toString =====================

    @Test
    fun `User toString contains class name`() {
        val user = makeUser()
        assertTrue(user.toString().contains("User"))
    }

    // ===================== defaults =====================

    @Test
    fun `User email defaults to empty string`() {
        val user = User(id = 1)
        assertEquals("", user.email)
    }

    @Test
    fun `User firstName defaults to empty string`() {
        val user = User(id = 1)
        assertEquals("", user.firstName)
    }

    @Test
    fun `User lastName defaults to empty string`() {
        val user = User(id = 1)
        assertEquals("", user.lastName)
    }

    @Test
    fun `User avatar defaults to empty string`() {
        val user = User(id = 1)
        assertEquals("", user.avatar)
    }

    @Test
    fun `User version defaults to 1`() {
        val user = User(id = 1)
        assertEquals(1, user.version)
    }

    @Test
    fun `User with only id - name falls back to email`() {
        val user = User(id = 1, email = "only@example.com")
        assertEquals("only@example.com", user.name)
    }

    // ===================== component functions =====================

    @Test
    fun `User component functions work`() {
        val user = makeUser(id = 5, email = "comp@example.com", firstName = "Comp", lastName = "Test")
        val (id, email, firstName, lastName, avatar) = user
        assertEquals(5, id)
        assertEquals("comp@example.com", email)
        assertEquals("Comp", firstName)
        assertEquals("Test", lastName)
        assertNotNull(avatar)
    }
}

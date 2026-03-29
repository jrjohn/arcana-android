package com.example.arcana.domain.validation

import com.example.arcana.R
import com.example.arcana.core.common.StringProvider
import com.example.arcana.domain.model.User
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserValidatorTest {

    private lateinit var stringProvider: StringProvider
    private lateinit var userValidator: UserValidator

    @Before
    fun setup() {
        stringProvider = mock()

        // Setup common string provider responses matching strings.xml
        whenever(stringProvider.getString(R.string.error_email_invalid)).thenReturn("Invalid email address format")
        whenever(stringProvider.getString(R.string.error_email_required)).thenReturn("Email address is required")
        whenever(stringProvider.getString(R.string.error_name_required)).thenReturn("First name or last name is required")
        whenever(stringProvider.getString(R.string.error_name_required_create)).thenReturn("Name is required")
        whenever(stringProvider.getString(R.string.error_first_name_too_long)).thenReturn("First name is too long (max 100 characters)")
        whenever(stringProvider.getString(R.string.error_last_name_too_long)).thenReturn("Last name is too long (max 100 characters)")
        whenever(stringProvider.getString(R.string.error_avatar_invalid)).thenReturn("Invalid avatar URL format")

        userValidator = UserValidator(stringProvider)
    }

    // ==================== validate() Tests ====================

    @Test
    fun `validate - valid user with all fields returns success`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            avatar = "https://example.com/avatar.jpg"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate - valid user with only first name returns success`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = "",
            avatar = ""
        )

        val result = userValidator.validate(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate - valid user with only last name returns success`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "",
            lastName = "Doe",
            avatar = ""
        )

        val result = userValidator.validate(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate - user with empty email fails validation`() {
        val user = User(
            id = 1,
            email = "",
            firstName = "John",
            lastName = "Doe"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isFailure)
        // Email validation error should be returned
        val message = result.exceptionOrNull()?.message
        assertTrue(message != null && (message.contains("email") || message.contains("Email")))
    }

    @Test
    fun `validate - user with invalid email format fails validation`() {
        val user = User(
            id = 1,
            email = "invalid-email",
            firstName = "John",
            lastName = "Doe"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isFailure)
    }

    @Test
    fun `validate - user with missing @ in email fails validation`() {
        val user = User(
            id = 1,
            email = "testexample.com",
            firstName = "John",
            lastName = "Doe"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isFailure)
    }

    @Test
    fun `validate - user with both names blank fails validation`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "",
            lastName = ""
        )

        val result = userValidator.validate(user)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("name") == true)
    }

    @Test
    fun `validate - user with both names whitespace fails validation`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "   ",
            lastName = "  "
        )

        val result = userValidator.validate(user)

        assertTrue(result.isFailure)
    }

    @Test
    fun `validate - user with firstName longer than 100 characters fails validation`() {
        val longName = "a".repeat(101)
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = longName,
            lastName = "Doe"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("First name is too long") == true)
    }

    @Test
    fun `validate - user with lastName longer than 100 characters fails validation`() {
        val longName = "a".repeat(101)
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = longName
        )

        val result = userValidator.validate(user)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Last name is too long") == true)
    }

    @Test
    fun `validate - user with firstName exactly 100 characters passes validation`() {
        val maxName = "a".repeat(100)
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = maxName,
            lastName = "Doe"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate - user with invalid avatar URL fails validation`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            avatar = "not-a-url"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid avatar URL") == true)
    }

    @Test
    fun `validate - user with avatar starting with http passes validation`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            avatar = "http://example.com/avatar.jpg"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate - user with avatar starting with https passes validation`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            avatar = "https://example.com/avatar.jpg"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate - user with empty avatar passes validation`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            avatar = ""
        )

        val result = userValidator.validate(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate - user with ftp protocol in avatar fails validation`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            avatar = "ftp://example.com/avatar.jpg"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isFailure)
    }

    // ==================== validateForCreation() Tests ====================

    @Test
    fun `validateForCreation - valid user with all fields returns success`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            avatar = "https://example.com/avatar.jpg"
        )

        val result = userValidator.validateForCreation(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateForCreation - user with empty email fails validation`() {
        val user = User(
            id = 1,
            email = "",
            firstName = "John",
            lastName = "Doe"
        )

        val result = userValidator.validateForCreation(user)

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message
        assertTrue(message != null && (message.contains("email") || message.contains("Email")))
    }

    @Test
    fun `validateForCreation - user with invalid email fails validation`() {
        val user = User(
            id = 1,
            email = "invalid-email",
            firstName = "John",
            lastName = "Doe"
        )

        val result = userValidator.validateForCreation(user)

        assertTrue(result.isFailure)
    }

    @Test
    fun `validateForCreation - user with both names blank fails validation`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "",
            lastName = ""
        )

        val result = userValidator.validateForCreation(user)

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message
        assertTrue(message != null && (message.contains("name") || message.contains("Name")))
    }

    @Test
    fun `validateForCreation - user with only first name passes validation`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = ""
        )

        val result = userValidator.validateForCreation(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateForCreation - user with only last name passes validation`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "",
            lastName = "Doe"
        )

        val result = userValidator.validateForCreation(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateForCreation - propagates other validation errors`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "a".repeat(101),
            lastName = "Doe"
        )

        val result = userValidator.validateForCreation(user)

        assertTrue(result.isFailure)
    }

    // ==================== validateForUpdate() Tests ====================

    @Test
    fun `validateForUpdate - delegates to validate method`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe"
        )

        val result = userValidator.validateForUpdate(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateForUpdate - fails for invalid user`() {
        val user = User(
            id = 1,
            email = "invalid-email",
            firstName = "John",
            lastName = "Doe"
        )

        val result = userValidator.validateForUpdate(user)

        assertTrue(result.isFailure)
    }

    // ==================== Companion Object Tests ====================

    @Test
    fun `validateEmail - valid email returns success`() {
        val result = UserValidator.validateEmail("test@example.com")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == "test@example.com")
    }

    @Test
    fun `validateEmail - invalid email returns failure`() {
        val result = UserValidator.validateEmail("invalid-email")

        assertTrue(result.isFailure)
    }

    @Test
    fun `validateEmail - empty email returns failure`() {
        val result = UserValidator.validateEmail("")

        assertTrue(result.isFailure)
    }

    @Test
    fun `validateEmail - email with spaces gets trimmed and validated`() {
        val result = UserValidator.validateEmail("  test@example.com  ")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == "test@example.com")
    }

    @Test
    fun `isValidName - non-blank name within length limit returns true`() {
        assertTrue(UserValidator.isValidName("John"))
    }

    @Test
    fun `isValidName - blank name returns false`() {
        assertFalse(UserValidator.isValidName(""))
        assertFalse(UserValidator.isValidName("   "))
    }

    @Test
    fun `isValidName - name longer than 100 characters returns false`() {
        val longName = "a".repeat(101)
        assertFalse(UserValidator.isValidName(longName))
    }

    @Test
    fun `isValidName - name exactly 100 characters returns true`() {
        val maxName = "a".repeat(100)
        assertTrue(UserValidator.isValidName(maxName))
    }

    @Test
    fun `isValidName - name with 1 character returns true`() {
        assertTrue(UserValidator.isValidName("A"))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `validate - email with special characters passes if valid format`() {
        val user = User(
            id = 1,
            email = "test.name+tag@example.co.uk",
            firstName = "John",
            lastName = "Doe"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate - multiple validation errors returns first error`() {
        val user = User(
            id = 1,
            email = "invalid-email",
            firstName = "a".repeat(101),
            lastName = "b".repeat(101),
            avatar = "not-a-url"
        )

        val result = userValidator.validate(user)

        assertTrue(result.isFailure)
        // Should return first error encountered
        assertTrue(result.exceptionOrNull() != null)
    }
}

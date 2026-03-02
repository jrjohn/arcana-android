package com.example.arcana.core.common

import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppErrorTest {

    // ==================== fromException Tests ====================

    @Test
    fun `fromException - IOException returns NetworkError`() {
        val exception = IOException("Connection failed")

        val error = AppError.fromException(exception)

        assertTrue(error is AppError.NetworkError)
        assertTrue(error.message.contains("Network error"))
        assertTrue(error.isRetryable)
        assertEquals(exception, error.throwable)
    }

    @Test
    fun `fromException - SocketTimeoutException returns NetworkError with timeout message`() {
        val exception = SocketTimeoutException("Timeout")

        val error = AppError.fromException(exception)

        assertTrue(error is AppError.NetworkError)
        assertEquals("Connection timed out", error.message)
        assertTrue(error.isRetryable)
        assertEquals(exception, error.throwable)
    }

    @Test
    fun `fromException - UnknownHostException returns NetworkError with no connection message`() {
        val exception = UnknownHostException("Unable to resolve host")

        val error = AppError.fromException(exception)

        assertTrue(error is AppError.NetworkError)
        assertEquals("No internet connection", error.message)
        assertTrue(error.isRetryable)
        assertEquals(exception, error.throwable)
    }

    @Test
    fun `fromException - IllegalStateException returns UnknownError`() {
        val exception = IllegalStateException("Invalid state")

        val error = AppError.fromException(exception)

        assertTrue(error is AppError.UnknownError)
        assertEquals("Invalid state", error.message)
        assertEquals(exception, error.throwable)
    }

    @Test
    fun `fromException - IllegalArgumentException returns UnknownError`() {
        val exception = IllegalArgumentException("Invalid argument")

        val error = AppError.fromException(exception)

        assertTrue(error is AppError.UnknownError)
        assertEquals("Invalid argument", error.message)
        assertEquals(exception, error.throwable)
    }

    @Test
    fun `fromException - NullPointerException returns UnknownError`() {
        val exception = NullPointerException("Null value")

        val error = AppError.fromException(exception)

        assertTrue(error is AppError.UnknownError)
        assertEquals("Null value", error.message)
        assertEquals(exception, error.throwable)
    }

    @Test
    fun `fromException - generic Exception with null message returns UnknownError with default message`() {
        val exception = Exception(null as String?)

        val error = AppError.fromException(exception)

        assertTrue(error is AppError.UnknownError)
        assertEquals("An unexpected error occurred", error.message)
        assertEquals(exception, error.throwable)
    }

    // ==================== Factory Methods Tests ====================

    @Test
    @Suppress("USELESS_IS_CHECK")
    fun `noConnection - creates NetworkError with offline message`() {
        val error = AppError.noConnection()

        assertTrue(error is AppError.NetworkError)
        assertTrue(error.message.contains("No internet connection"))
        assertTrue(error.message.contains("synced when online"))
        assertTrue(error.isRetryable)
        assertEquals(null, error.throwable)
    }

    @Test
    @Suppress("USELESS_IS_CHECK")
    fun `validation - creates ValidationError with field and message`() {
        val error = AppError.validation("email", "Invalid email format")

        assertTrue(error is AppError.ValidationError)
        assertEquals("email", error.field)
        assertEquals("Invalid email format", error.message)
        assertEquals(null, error.throwable)
    }

    @Test
    @Suppress("USELESS_IS_CHECK")
    fun `validation - creates ValidationError with different field`() {
        val error = AppError.validation("password", "Password too short")

        assertTrue(error is AppError.ValidationError)
        assertEquals("password", error.field)
        assertEquals("Password too short", error.message)
    }

    // ==================== fromHttpCode Tests ====================

    @Test
    fun `fromHttpCode - 401 returns AuthError`() {
        val error = AppError.fromHttpCode(401)

        assertTrue(error is AppError.AuthError)
        assertEquals("Authentication required", error.message)
    }

    @Test
    fun `fromHttpCode - 401 with custom message returns AuthError`() {
        val error = AppError.fromHttpCode(401, "Token expired")

        assertTrue(error is AppError.AuthError)
        assertEquals("Token expired", error.message)
    }

    @Test
    fun `fromHttpCode - 403 returns AuthError`() {
        val error = AppError.fromHttpCode(403)

        assertTrue(error is AppError.AuthError)
        assertEquals("Access forbidden", error.message)
    }

    @Test
    fun `fromHttpCode - 403 with custom message returns AuthError`() {
        val error = AppError.fromHttpCode(403, "Access forbidden")

        assertTrue(error is AppError.AuthError)
        assertEquals("Access forbidden", error.message)
    }

    @Test
    fun `fromHttpCode - 409 returns ConflictError`() {
        val error = AppError.fromHttpCode(409)

        assertTrue(error is AppError.ConflictError)
        assertEquals("Data conflict detected", error.message)
    }

    @Test
    fun `fromHttpCode - 409 with custom message returns ConflictError`() {
        val error = AppError.fromHttpCode(409, "Version mismatch")

        assertTrue(error is AppError.ConflictError)
        assertEquals("Version mismatch", error.message)
    }

    @Test
    fun `fromHttpCode - 400 returns ServerError`() {
        val error = AppError.fromHttpCode(400)

        assertTrue(error is AppError.ServerError)
        assertEquals(400, error.code)
        assertEquals("Client error: 400", error.message)
    }

    @Test
    fun `fromHttpCode - 404 returns ServerError`() {
        val error = AppError.fromHttpCode(404)

        assertTrue(error is AppError.ServerError)
        assertEquals(404, error.code)
        assertEquals("Resource not found", error.message)
    }

    @Test
    fun `fromHttpCode - 404 with custom message returns ServerError`() {
        val error = AppError.fromHttpCode(404, "User not found")

        assertTrue(error is AppError.ServerError)
        assertEquals(404, error.code)
        assertEquals("User not found", error.message)
    }

    @Test
    fun `fromHttpCode - 422 returns ServerError`() {
        val error = AppError.fromHttpCode(422)

        assertTrue(error is AppError.ServerError)
        assertEquals(422, error.code)
        assertEquals("Client error: 422", error.message)
    }

    @Test
    fun `fromHttpCode - 500 returns ServerError`() {
        val error = AppError.fromHttpCode(500)

        assertTrue(error is AppError.ServerError)
        assertEquals(500, error.code)
        assertEquals("Server error: 500", error.message)
    }

    @Test
    fun `fromHttpCode - 502 returns ServerError`() {
        val error = AppError.fromHttpCode(502)

        assertTrue(error is AppError.ServerError)
        assertEquals(502, error.code)
        assertEquals("Server error: 502", error.message)
    }

    @Test
    fun `fromHttpCode - 503 with custom message returns ServerError`() {
        val error = AppError.fromHttpCode(503, "Service temporarily unavailable")

        assertTrue(error is AppError.ServerError)
        assertEquals(503, error.code)
        assertEquals("Service temporarily unavailable", error.message)
    }

    @Test
    fun `fromHttpCode - 599 returns ServerError`() {
        val error = AppError.fromHttpCode(599)

        assertTrue(error is AppError.ServerError)
        assertEquals(599, error.code)
        assertEquals("Server error: 599", error.message)
    }

    @Test
    fun `fromHttpCode - 200 returns UnknownError`() {
        val error = AppError.fromHttpCode(200)

        assertTrue(error is AppError.UnknownError)
        assertTrue(error.message.contains("Unexpected HTTP code: 200"))
    }

    @Test
    fun `fromHttpCode - 300 returns UnknownError`() {
        val error = AppError.fromHttpCode(300)

        assertTrue(error is AppError.UnknownError)
        assertTrue(error.message.contains("Unexpected HTTP code: 300"))
    }

    @Test
    fun `fromHttpCode - 600 returns UnknownError`() {
        val error = AppError.fromHttpCode(600)

        assertTrue(error is AppError.UnknownError)
        assertTrue(error.message.contains("Unexpected HTTP code: 600"))
    }

    // ==================== getUserMessage Extension Tests ====================

    @Test
    fun `getUserMessage - NetworkError with retryable true appends try again`() {
        val error = AppError.NetworkError(message = "Connection lost", isRetryable = true)

        val message = error.getUserMessage()

        assertTrue(message.contains("Connection lost"))
        assertTrue(message.contains("Please try again"))
    }

    @Test
    fun `getUserMessage - NetworkError with retryable false does not append try again`() {
        val error = AppError.NetworkError(message = "Permanent failure", isRetryable = false)

        val message = error.getUserMessage()

        assertEquals("Permanent failure", message)
        assertFalse(message.contains("Please try again"))
    }

    @Test
    fun `getUserMessage - ValidationError returns message as is`() {
        val error = AppError.validation("email", "Invalid email format")

        val message = error.getUserMessage()

        assertEquals("Invalid email format", message)
    }

    @Test
    fun `getUserMessage - ServerError with 500 code returns generic server message`() {
        val error = AppError.ServerError(code = 500, message = "Internal server error")

        val message = error.getUserMessage()

        assertTrue(message.contains("Server is experiencing issues"))
        assertTrue(message.contains("try again later"))
    }

    @Test
    fun `getUserMessage - ServerError with 502 code returns generic server message`() {
        val error = AppError.ServerError(code = 502, message = "Bad gateway")

        val message = error.getUserMessage()

        assertTrue(message.contains("Server is experiencing issues"))
    }

    @Test
    fun `getUserMessage - ServerError with 599 code returns generic server message`() {
        val error = AppError.ServerError(code = 599, message = "Custom server error")

        val message = error.getUserMessage()

        assertTrue(message.contains("Server is experiencing issues"))
    }

    @Test
    fun `getUserMessage - ServerError with 400 code returns original message`() {
        val error = AppError.ServerError(code = 400, message = "Bad request")

        val message = error.getUserMessage()

        assertEquals("Bad request", message)
    }

    @Test
    fun `getUserMessage - ServerError with 404 code returns original message`() {
        val error = AppError.ServerError(code = 404, message = "Not found")

        val message = error.getUserMessage()

        assertEquals("Not found", message)
    }

    @Test
    fun `getUserMessage - ConflictError appends refresh message`() {
        val error = AppError.ConflictError(message = "Version mismatch")

        val message = error.getUserMessage()

        assertTrue(message.contains("Version mismatch"))
        assertTrue(message.contains("Please refresh and try again"))
    }

    @Test
    fun `getUserMessage - AuthError appends sign in message`() {
        val error = AppError.AuthError(message = "Token expired")

        val message = error.getUserMessage()

        assertTrue(message.contains("Token expired"))
        assertTrue(message.contains("Please sign in again"))
    }

    @Test
    fun `getUserMessage - UnknownError returns generic message`() {
        val error = AppError.UnknownError(message = "Something bad happened", throwable = Exception())

        val message = error.getUserMessage()

        assertEquals("Something went wrong. Please try again.", message)
    }

    // ==================== isRetryable Extension Tests ====================

    @Test
    fun `isRetryable - NetworkError with isRetryable true returns true`() {
        val error = AppError.NetworkError(message = "Error", isRetryable = true)

        assertTrue(error.isRetryable())
    }

    @Test
    fun `isRetryable - NetworkError with isRetryable false returns false`() {
        val error = AppError.NetworkError(message = "Error", isRetryable = false)

        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable - ServerError with 500 code returns true`() {
        val error = AppError.ServerError(code = 500, message = "Internal server error")

        assertTrue(error.isRetryable())
    }

    @Test
    fun `isRetryable - ServerError with 502 code returns true`() {
        val error = AppError.ServerError(code = 502, message = "Bad gateway")

        assertTrue(error.isRetryable())
    }

    @Test
    fun `isRetryable - ServerError with 503 code returns true`() {
        val error = AppError.ServerError(code = 503, message = "Service unavailable")

        assertTrue(error.isRetryable())
    }

    @Test
    fun `isRetryable - ServerError with 599 code returns true`() {
        val error = AppError.ServerError(code = 599, message = "Custom error")

        assertTrue(error.isRetryable())
    }

    @Test
    fun `isRetryable - ServerError with 400 code returns false`() {
        val error = AppError.ServerError(code = 400, message = "Bad request")

        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable - ServerError with 404 code returns false`() {
        val error = AppError.ServerError(code = 404, message = "Not found")

        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable - ServerError with 422 code returns false`() {
        val error = AppError.ServerError(code = 422, message = "Unprocessable entity")

        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable - ConflictError returns true`() {
        val error = AppError.ConflictError(message = "Conflict")

        assertTrue(error.isRetryable())
    }

    @Test
    fun `isRetryable - ValidationError returns false`() {
        val error = AppError.validation("email", "Invalid")

        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable - AuthError returns false`() {
        val error = AppError.AuthError(message = "Unauthorized")

        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable - UnknownError returns false`() {
        val error = AppError.UnknownError(message = "Unknown", throwable = Exception())

        assertFalse(error.isRetryable())
    }

    // ==================== Edge Cases ====================

    @Test
    fun `NetworkError - can have custom throwable`() {
        val throwable = IOException("Custom exception")
        val error = AppError.NetworkError(message = "Network error", throwable = throwable)

        assertEquals(throwable, error.throwable)
    }

    @Test
    fun `ValidationError - can have custom throwable`() {
        val throwable = Exception("Validation exception")
        val error = AppError.ValidationError(field = "field", message = "Invalid", throwable = throwable)

        assertEquals(throwable, error.throwable)
    }

    @Test
    fun `ServerError - can have custom throwable`() {
        val throwable = Exception("Server exception")
        val error = AppError.ServerError(code = 500, message = "Error", throwable = throwable)

        assertEquals(throwable, error.throwable)
    }

    @Test
    fun `ConflictError - can have custom throwable`() {
        val throwable = Exception("Conflict exception")
        val error = AppError.ConflictError(message = "Conflict", throwable = throwable)

        assertEquals(throwable, error.throwable)
    }

    @Test
    fun `AuthError - can have custom throwable`() {
        val throwable = Exception("Auth exception")
        val error = AppError.AuthError(message = "Unauthorized", throwable = throwable)

        assertEquals(throwable, error.throwable)
    }
}

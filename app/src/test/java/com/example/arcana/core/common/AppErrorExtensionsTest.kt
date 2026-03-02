package com.example.arcana.core.common

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for AppError sealed class, extension functions getUserMessage() and isRetryable(),
 * and the AppError.fromException / fromHttpCode / noConnection factory methods.
 */
class AppErrorExtensionsTest {

    // ========== getUserMessage() Extension Tests ==========

    @Test
    fun `getUserMessage for NetworkError retryable appends please try again`() {
        val error = AppError.NetworkError(message = "No internet connection", isRetryable = true)
        val message = error.getUserMessage()
        assertTrue(message.endsWith("Please try again."), "Actual: $message")
    }

    @Test
    fun `getUserMessage for NetworkError non-retryable returns just message`() {
        val error = AppError.NetworkError(message = "Connection refused", isRetryable = false)
        assertEquals("Connection refused", error.getUserMessage())
    }

    @Test
    fun `getUserMessage for ValidationError returns message as-is`() {
        val error = AppError.ValidationError(field = "email", message = "Invalid email format")
        assertEquals("Invalid email format", error.getUserMessage())
    }

    @Test
    fun `getUserMessage for ServerError 500 returns server issues message`() {
        val error = AppError.ServerError(code = 500, message = "Internal Server Error")
        assertEquals("Server is experiencing issues. Please try again later.", error.getUserMessage())
    }

    @Test
    fun `getUserMessage for ServerError 503 returns server issues message`() {
        val error = AppError.ServerError(code = 503, message = "Service Unavailable")
        assertEquals("Server is experiencing issues. Please try again later.", error.getUserMessage())
    }

    @Test
    fun `getUserMessage for ServerError 404 returns message directly`() {
        val error = AppError.ServerError(code = 404, message = "Resource not found")
        assertEquals("Resource not found", error.getUserMessage())
    }

    @Test
    fun `getUserMessage for ServerError 400 returns message directly`() {
        val error = AppError.ServerError(code = 400, message = "Bad Request")
        assertEquals("Bad Request", error.getUserMessage())
    }

    @Test
    fun `getUserMessage for ConflictError appends refresh and try again`() {
        val error = AppError.ConflictError(message = "Data conflict detected")
        assertTrue(error.getUserMessage().contains("Please refresh and try again."))
    }

    @Test
    fun `getUserMessage for AuthError appends sign in message`() {
        val error = AppError.AuthError(message = "Authentication required")
        assertTrue(error.getUserMessage().endsWith("Please sign in again."))
    }

    @Test
    fun `getUserMessage for UnknownError returns something went wrong`() {
        val error = AppError.UnknownError(throwable = RuntimeException("unexpected"))
        assertEquals("Something went wrong. Please try again.", error.getUserMessage())
    }

    // ========== isRetryable() Extension Tests ==========

    @Test
    fun `isRetryable returns true for NetworkError with isRetryable=true`() {
        val error = AppError.NetworkError(message = "timeout", isRetryable = true)
        assertTrue(error.isRetryable())
    }

    @Test
    fun `isRetryable returns false for NetworkError with isRetryable=false`() {
        val error = AppError.NetworkError(message = "auth error", isRetryable = false)
        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable returns true for ServerError 500`() {
        val error = AppError.ServerError(code = 500, message = "Server Error")
        assertTrue(error.isRetryable())
    }

    @Test
    fun `isRetryable returns true for ServerError 503`() {
        val error = AppError.ServerError(code = 503, message = "Service Unavailable")
        assertTrue(error.isRetryable())
    }

    @Test
    fun `isRetryable returns false for ServerError 404`() {
        val error = AppError.ServerError(code = 404, message = "Not Found")
        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable returns false for ServerError 400`() {
        val error = AppError.ServerError(code = 400, message = "Bad Request")
        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable returns true for ConflictError`() {
        val error = AppError.ConflictError(message = "conflict")
        assertTrue(error.isRetryable())
    }

    @Test
    fun `isRetryable returns false for ValidationError`() {
        val error = AppError.ValidationError(field = "email", message = "invalid")
        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable returns false for AuthError`() {
        val error = AppError.AuthError(message = "unauthorized")
        assertFalse(error.isRetryable())
    }

    @Test
    fun `isRetryable returns false for UnknownError`() {
        val error = AppError.UnknownError(throwable = RuntimeException())
        assertFalse(error.isRetryable())
    }

    // ========== AppError.fromException() Tests ==========

    @Test
    fun `fromException with UnknownHostException returns NetworkError`() {
        val ex = java.net.UnknownHostException("api.example.com")
        val error = AppError.fromException(ex)
        assertIs<AppError.NetworkError>(error)
        assertTrue((error as AppError.NetworkError).isRetryable)
    }

    @Test
    fun `fromException with SocketTimeoutException returns NetworkError`() {
        val ex = java.net.SocketTimeoutException("timed out")
        val error = AppError.fromException(ex)
        assertIs<AppError.NetworkError>(error)
        assertTrue((error as AppError.NetworkError).isRetryable)
    }

    @Test
    fun `fromException with IOException returns NetworkError`() {
        val ex = java.io.IOException("connection reset")
        val error = AppError.fromException(ex)
        assertIs<AppError.NetworkError>(error)
    }

    @Test
    fun `fromException with generic Exception returns UnknownError`() {
        val ex = RuntimeException("unexpected error")
        val error = AppError.fromException(ex)
        assertIs<AppError.UnknownError>(error)
    }

    @Test
    fun `fromException with IllegalStateException returns UnknownError`() {
        val ex = IllegalStateException("illegal state")
        val error = AppError.fromException(ex)
        assertIs<AppError.UnknownError>(error)
    }

    @Test
    fun `fromException UnknownHostException has correct error code`() {
        val ex = java.net.UnknownHostException("host")
        val error = AppError.fromException(ex) as AppError.NetworkError
        assertEquals(ErrorCode.E1002_UNKNOWN_HOST, error.errorCode)
    }

    @Test
    fun `fromException SocketTimeoutException has correct error code`() {
        val ex = java.net.SocketTimeoutException()
        val error = AppError.fromException(ex) as AppError.NetworkError
        assertEquals(ErrorCode.E1001_CONNECTION_TIMEOUT, error.errorCode)
    }

    // ========== AppError.noConnection() Tests ==========

    @Test
    fun `noConnection returns NetworkError`() {
        val error = AppError.noConnection()
        assertIs<AppError.NetworkError>(error)
    }

    @Test
    fun `noConnection isRetryable is true`() {
        val error = AppError.noConnection()
        assertTrue(error.isRetryable)
    }

    @Test
    fun `noConnection has correct error code`() {
        val error = AppError.noConnection()
        assertEquals(ErrorCode.E1000_NO_CONNECTION, error.errorCode)
    }

    @Test
    fun `noConnection throwable is null`() {
        val error = AppError.noConnection()
        assertTrue(error.throwable == null)
    }

    // ========== AppError.validation() Tests ==========

    @Test
    fun `validation creates ValidationError with field and message`() {
        val error = AppError.validation("email", "Email is required")
        assertIs<AppError.ValidationError>(error)
        assertEquals("email", error.field)
        assertEquals("Email is required", error.message)
    }

    @Test
    fun `validation has default error code`() {
        val error = AppError.validation("field", "message")
        assertEquals(ErrorCode.E2000_VALIDATION_FAILED, error.errorCode)
    }

    @Test
    fun `validation with custom error code`() {
        val error = AppError.validation(
            "name",
            "Name too short",
            ErrorCode.E2001_REQUIRED_FIELD
        )
        assertEquals(ErrorCode.E2001_REQUIRED_FIELD, error.errorCode)
    }

    // ========== AppError.fromHttpCode() Tests ==========

    @Test
    fun `fromHttpCode 401 returns AuthError`() {
        val error = AppError.fromHttpCode(401)
        assertIs<AppError.AuthError>(error)
    }

    @Test
    fun `fromHttpCode 403 returns AuthError`() {
        val error = AppError.fromHttpCode(403)
        assertIs<AppError.AuthError>(error)
    }

    @Test
    fun `fromHttpCode 404 returns ServerError with code 404`() {
        val error = AppError.fromHttpCode(404)
        assertIs<AppError.ServerError>(error)
        assertEquals(404, (error as AppError.ServerError).code)
    }

    @Test
    fun `fromHttpCode 409 returns ConflictError`() {
        val error = AppError.fromHttpCode(409)
        assertIs<AppError.ConflictError>(error)
    }

    @Test
    fun `fromHttpCode 429 returns ServerError`() {
        val error = AppError.fromHttpCode(429)
        assertIs<AppError.ServerError>(error)
    }

    @Test
    fun `fromHttpCode 400 returns ServerError`() {
        val error = AppError.fromHttpCode(400)
        assertIs<AppError.ServerError>(error)
    }

    @Test
    fun `fromHttpCode 500 returns ServerError`() {
        val error = AppError.fromHttpCode(500)
        assertIs<AppError.ServerError>(error)
        assertEquals(500, (error as AppError.ServerError).code)
    }

    @Test
    fun `fromHttpCode 503 returns ServerError`() {
        val error = AppError.fromHttpCode(503)
        assertIs<AppError.ServerError>(error)
        assertEquals(503, (error as AppError.ServerError).code)
    }

    @Test
    fun `fromHttpCode 200 returns UnknownError`() {
        val error = AppError.fromHttpCode(200)
        assertIs<AppError.UnknownError>(error)
    }

    @Test
    fun `fromHttpCode with custom message`() {
        val error = AppError.fromHttpCode(404, "Custom not found message")
        assertIs<AppError.ServerError>(error)
        assertEquals("Custom not found message", (error as AppError.ServerError).message)
    }

    @Test
    fun `fromHttpCode 401 with custom message`() {
        val error = AppError.fromHttpCode(401, "Session expired")
        assertIs<AppError.AuthError>(error)
        assertEquals("Session expired", (error as AppError.AuthError).message)
    }

    @Test
    fun `fromHttpCode 403 error code is forbidden`() {
        val error = AppError.fromHttpCode(403) as AppError.AuthError
        assertEquals(ErrorCode.E4002_FORBIDDEN, error.errorCode)
    }

    @Test
    fun `fromHttpCode 401 error code is unauthorized`() {
        val error = AppError.fromHttpCode(401) as AppError.AuthError
        assertEquals(ErrorCode.E4001_UNAUTHORIZED, error.errorCode)
    }

    @Test
    fun `fromHttpCode 429 has rate limited error code`() {
        val error = AppError.fromHttpCode(429) as AppError.ServerError
        assertEquals(ErrorCode.E3004_RATE_LIMITED, error.errorCode)
    }

    @Test
    fun `fromHttpCode 503 has service unavailable error code`() {
        val error = AppError.fromHttpCode(503) as AppError.ServerError
        assertEquals(ErrorCode.E3003_SERVICE_UNAVAILABLE, error.errorCode)
    }
}

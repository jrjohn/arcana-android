package com.example.arcana.core.common

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorCodeTest {

    // ========== Network Error Codes ==========

    @Test
    fun `E1000_NO_CONNECTION has correct code`() {
        assertEquals("E1000", ErrorCode.E1000_NO_CONNECTION.code)
    }

    @Test
    fun `E1000_NO_CONNECTION has correct description`() {
        assertEquals("No internet connection available", ErrorCode.E1000_NO_CONNECTION.description)
    }

    @Test
    fun `E1000_NO_CONNECTION has correct category`() {
        assertEquals("Network", ErrorCode.E1000_NO_CONNECTION.category)
    }

    @Test
    fun `E1001_CONNECTION_TIMEOUT has correct code`() {
        assertEquals("E1001", ErrorCode.E1001_CONNECTION_TIMEOUT.code)
    }

    @Test
    fun `E1001_CONNECTION_TIMEOUT has correct description`() {
        assertEquals("Connection attempt timed out", ErrorCode.E1001_CONNECTION_TIMEOUT.description)
    }

    @Test
    fun `E1001_CONNECTION_TIMEOUT has correct category`() {
        assertEquals("Network", ErrorCode.E1001_CONNECTION_TIMEOUT.category)
    }

    @Test
    fun `E1002_UNKNOWN_HOST has correct code`() {
        assertEquals("E1002", ErrorCode.E1002_UNKNOWN_HOST.code)
    }

    @Test
    fun `E1002_UNKNOWN_HOST has correct description`() {
        assertEquals("Unable to resolve host address", ErrorCode.E1002_UNKNOWN_HOST.description)
    }

    @Test
    fun `E1002_UNKNOWN_HOST has correct category`() {
        assertEquals("Network", ErrorCode.E1002_UNKNOWN_HOST.category)
    }

    @Test
    fun `E1003_NETWORK_IO has correct code`() {
        assertEquals("E1003", ErrorCode.E1003_NETWORK_IO.code)
    }

    @Test
    fun `E1003_NETWORK_IO has correct description`() {
        assertEquals("Network I/O error occurred", ErrorCode.E1003_NETWORK_IO.description)
    }

    @Test
    fun `E1003_NETWORK_IO has correct category`() {
        assertEquals("Network", ErrorCode.E1003_NETWORK_IO.category)
    }

    @Test
    fun `E1004_SSL_ERROR has correct code`() {
        assertEquals("E1004", ErrorCode.E1004_SSL_ERROR.code)
    }

    @Test
    fun `E1004_SSL_ERROR has correct description`() {
        assertEquals("SSL/TLS connection error", ErrorCode.E1004_SSL_ERROR.description)
    }

    @Test
    fun `E1004_SSL_ERROR has correct category`() {
        assertEquals("Network", ErrorCode.E1004_SSL_ERROR.category)
    }

    // ========== Validation Error Codes ==========

    @Test
    fun `E2000_VALIDATION_FAILED has correct properties`() {
        assertEquals("E2000", ErrorCode.E2000_VALIDATION_FAILED.code)
        assertEquals("Input validation failed", ErrorCode.E2000_VALIDATION_FAILED.description)
        assertEquals("Validation", ErrorCode.E2000_VALIDATION_FAILED.category)
    }

    @Test
    fun `E2001_INVALID_EMAIL has correct properties`() {
        assertEquals("E2001", ErrorCode.E2001_INVALID_EMAIL.code)
        assertEquals("Invalid email address format", ErrorCode.E2001_INVALID_EMAIL.description)
        assertEquals("Validation", ErrorCode.E2001_INVALID_EMAIL.category)
    }

    @Test
    fun `E2002_INVALID_NAME has correct properties`() {
        assertEquals("E2002", ErrorCode.E2002_INVALID_NAME.code)
        assertEquals("Invalid name format or length", ErrorCode.E2002_INVALID_NAME.description)
        assertEquals("Validation", ErrorCode.E2002_INVALID_NAME.category)
    }

    @Test
    fun `E2003_REQUIRED_FIELD has correct properties`() {
        assertEquals("E2003", ErrorCode.E2003_REQUIRED_FIELD.code)
        assertEquals("Required field is missing", ErrorCode.E2003_REQUIRED_FIELD.description)
        assertEquals("Validation", ErrorCode.E2003_REQUIRED_FIELD.category)
    }

    @Test
    fun `E2004_FIELD_TOO_LONG has correct properties`() {
        assertEquals("E2004", ErrorCode.E2004_FIELD_TOO_LONG.code)
        assertEquals("Field value exceeds maximum length", ErrorCode.E2004_FIELD_TOO_LONG.description)
        assertEquals("Validation", ErrorCode.E2004_FIELD_TOO_LONG.category)
    }

    @Test
    fun `E2005_FIELD_TOO_SHORT has correct properties`() {
        assertEquals("E2005", ErrorCode.E2005_FIELD_TOO_SHORT.code)
        assertEquals("Field value below minimum length", ErrorCode.E2005_FIELD_TOO_SHORT.description)
        assertEquals("Validation", ErrorCode.E2005_FIELD_TOO_SHORT.category)
    }

    // ========== Server Error Codes ==========

    @Test
    fun `E3000_SERVER_ERROR has correct properties`() {
        assertEquals("E3000", ErrorCode.E3000_SERVER_ERROR.code)
        assertEquals("Internal server error", ErrorCode.E3000_SERVER_ERROR.description)
        assertEquals("Server", ErrorCode.E3000_SERVER_ERROR.category)
    }

    @Test
    fun `E3001_BAD_REQUEST has correct properties`() {
        assertEquals("E3001", ErrorCode.E3001_BAD_REQUEST.code)
        assertEquals("Bad request - invalid parameters", ErrorCode.E3001_BAD_REQUEST.description)
        assertEquals("Server", ErrorCode.E3001_BAD_REQUEST.category)
    }

    @Test
    fun `E3002_NOT_FOUND has correct properties`() {
        assertEquals("E3002", ErrorCode.E3002_NOT_FOUND.code)
        assertEquals("Resource not found", ErrorCode.E3002_NOT_FOUND.description)
        assertEquals("Server", ErrorCode.E3002_NOT_FOUND.category)
    }

    @Test
    fun `E3003_SERVICE_UNAVAILABLE has correct properties`() {
        assertEquals("E3003", ErrorCode.E3003_SERVICE_UNAVAILABLE.code)
        assertEquals("Service temporarily unavailable", ErrorCode.E3003_SERVICE_UNAVAILABLE.description)
        assertEquals("Server", ErrorCode.E3003_SERVICE_UNAVAILABLE.category)
    }

    @Test
    fun `E3004_RATE_LIMITED has correct properties`() {
        assertEquals("E3004", ErrorCode.E3004_RATE_LIMITED.code)
        assertEquals("Rate limit exceeded", ErrorCode.E3004_RATE_LIMITED.description)
        assertEquals("Server", ErrorCode.E3004_RATE_LIMITED.category)
    }

    // ========== Authentication Error Codes ==========

    @Test
    fun `E4000_AUTH_REQUIRED has correct properties`() {
        assertEquals("E4000", ErrorCode.E4000_AUTH_REQUIRED.code)
        assertEquals("Authentication required", ErrorCode.E4000_AUTH_REQUIRED.description)
        assertEquals("Authentication", ErrorCode.E4000_AUTH_REQUIRED.category)
    }

    @Test
    fun `E4001_UNAUTHORIZED has correct properties`() {
        assertEquals("E4001", ErrorCode.E4001_UNAUTHORIZED.code)
        assertEquals("Invalid credentials", ErrorCode.E4001_UNAUTHORIZED.description)
        assertEquals("Authentication", ErrorCode.E4001_UNAUTHORIZED.category)
    }

    @Test
    fun `E4002_FORBIDDEN has correct properties`() {
        assertEquals("E4002", ErrorCode.E4002_FORBIDDEN.code)
        assertEquals("Access forbidden - insufficient permissions", ErrorCode.E4002_FORBIDDEN.description)
        assertEquals("Authentication", ErrorCode.E4002_FORBIDDEN.category)
    }

    @Test
    fun `E4003_SESSION_EXPIRED has correct properties`() {
        assertEquals("E4003", ErrorCode.E4003_SESSION_EXPIRED.code)
        assertEquals("Session has expired", ErrorCode.E4003_SESSION_EXPIRED.description)
        assertEquals("Authentication", ErrorCode.E4003_SESSION_EXPIRED.category)
    }

    @Test
    fun `E4004_TOKEN_INVALID has correct properties`() {
        assertEquals("E4004", ErrorCode.E4004_TOKEN_INVALID.code)
        assertEquals("Invalid or malformed token", ErrorCode.E4004_TOKEN_INVALID.description)
        assertEquals("Authentication", ErrorCode.E4004_TOKEN_INVALID.category)
    }

    // ========== Data/Conflict Error Codes ==========

    @Test
    fun `E5000_DATA_CONFLICT has correct properties`() {
        assertEquals("E5000", ErrorCode.E5000_DATA_CONFLICT.code)
        assertEquals("Data conflict detected", ErrorCode.E5000_DATA_CONFLICT.description)
        assertEquals("Data", ErrorCode.E5000_DATA_CONFLICT.category)
    }

    @Test
    fun `E5001_STALE_DATA has correct properties`() {
        assertEquals("E5001", ErrorCode.E5001_STALE_DATA.code)
        assertEquals("Data has been modified by another user", ErrorCode.E5001_STALE_DATA.description)
        assertEquals("Data", ErrorCode.E5001_STALE_DATA.category)
    }

    @Test
    fun `E5002_DUPLICATE_ENTRY has correct properties`() {
        assertEquals("E5002", ErrorCode.E5002_DUPLICATE_ENTRY.code)
        assertEquals("Duplicate entry detected", ErrorCode.E5002_DUPLICATE_ENTRY.description)
        assertEquals("Data", ErrorCode.E5002_DUPLICATE_ENTRY.category)
    }

    @Test
    fun `E5003_CONSTRAINT_VIOLATION has correct properties`() {
        assertEquals("E5003", ErrorCode.E5003_CONSTRAINT_VIOLATION.code)
        assertEquals("Database constraint violation", ErrorCode.E5003_CONSTRAINT_VIOLATION.description)
        assertEquals("Data", ErrorCode.E5003_CONSTRAINT_VIOLATION.category)
    }

    // ========== Database Error Codes ==========

    @Test
    fun `E6000_DATABASE_ERROR has correct properties`() {
        assertEquals("E6000", ErrorCode.E6000_DATABASE_ERROR.code)
        assertEquals("Database operation failed", ErrorCode.E6000_DATABASE_ERROR.description)
        assertEquals("Database", ErrorCode.E6000_DATABASE_ERROR.category)
    }

    @Test
    fun `E6001_QUERY_FAILED has correct properties`() {
        assertEquals("E6001", ErrorCode.E6001_QUERY_FAILED.code)
        assertEquals("Database query execution failed", ErrorCode.E6001_QUERY_FAILED.description)
        assertEquals("Database", ErrorCode.E6001_QUERY_FAILED.category)
    }

    @Test
    fun `E6002_TRANSACTION_FAILED has correct properties`() {
        assertEquals("E6002", ErrorCode.E6002_TRANSACTION_FAILED.code)
        assertEquals("Database transaction failed", ErrorCode.E6002_TRANSACTION_FAILED.description)
        assertEquals("Database", ErrorCode.E6002_TRANSACTION_FAILED.category)
    }

    @Test
    fun `E6003_MIGRATION_FAILED has correct properties`() {
        assertEquals("E6003", ErrorCode.E6003_MIGRATION_FAILED.code)
        assertEquals("Database migration failed", ErrorCode.E6003_MIGRATION_FAILED.description)
        assertEquals("Database", ErrorCode.E6003_MIGRATION_FAILED.category)
    }

    // ========== System Error Codes ==========

    @Test
    fun `E9000_UNKNOWN has correct properties`() {
        assertEquals("E9000", ErrorCode.E9000_UNKNOWN.code)
        assertEquals("Unknown error occurred", ErrorCode.E9000_UNKNOWN.description)
        assertEquals("System", ErrorCode.E9000_UNKNOWN.category)
    }

    @Test
    fun `E9001_UNEXPECTED_STATE has correct properties`() {
        assertEquals("E9001", ErrorCode.E9001_UNEXPECTED_STATE.code)
        assertEquals("Unexpected application state", ErrorCode.E9001_UNEXPECTED_STATE.description)
        assertEquals("System", ErrorCode.E9001_UNEXPECTED_STATE.category)
    }

    @Test
    fun `E9002_NULL_POINTER has correct properties`() {
        assertEquals("E9002", ErrorCode.E9002_NULL_POINTER.code)
        assertEquals("Null pointer exception", ErrorCode.E9002_NULL_POINTER.description)
        assertEquals("System", ErrorCode.E9002_NULL_POINTER.category)
    }

    @Test
    fun `E9003_SERIALIZATION_ERROR has correct properties`() {
        assertEquals("E9003", ErrorCode.E9003_SERIALIZATION_ERROR.code)
        assertEquals("Data serialization/deserialization failed", ErrorCode.E9003_SERIALIZATION_ERROR.description)
        assertEquals("System", ErrorCode.E9003_SERIALIZATION_ERROR.category)
    }

    // ========== Warning Codes ==========

    @Test
    fun `W1000_SLOW_CONNECTION has correct properties`() {
        assertEquals("W1000", ErrorCode.W1000_SLOW_CONNECTION.code)
        assertEquals("Network connection is slow", ErrorCode.W1000_SLOW_CONNECTION.description)
        assertEquals("Network", ErrorCode.W1000_SLOW_CONNECTION.category)
    }

    @Test
    fun `W1001_OFFLINE_MODE has correct properties`() {
        assertEquals("W1001", ErrorCode.W1001_OFFLINE_MODE.code)
        assertEquals("Operating in offline mode", ErrorCode.W1001_OFFLINE_MODE.description)
        assertEquals("Network", ErrorCode.W1001_OFFLINE_MODE.category)
    }

    @Test
    fun `W1002_SYNC_PENDING has correct properties`() {
        assertEquals("W1002", ErrorCode.W1002_SYNC_PENDING.code)
        assertEquals("Data synchronization pending", ErrorCode.W1002_SYNC_PENDING.description)
        assertEquals("Network", ErrorCode.W1002_SYNC_PENDING.category)
    }

    @Test
    fun `W2000_INCOMPLETE_DATA has correct properties`() {
        assertEquals("W2000", ErrorCode.W2000_INCOMPLETE_DATA.code)
        assertEquals("Data is incomplete but acceptable", ErrorCode.W2000_INCOMPLETE_DATA.description)
        assertEquals("Validation", ErrorCode.W2000_INCOMPLETE_DATA.category)
    }

    @Test
    fun `W2001_DEPRECATED_FORMAT has correct properties`() {
        assertEquals("W2001", ErrorCode.W2001_DEPRECATED_FORMAT.code)
        assertEquals("Using deprecated data format", ErrorCode.W2001_DEPRECATED_FORMAT.description)
        assertEquals("Validation", ErrorCode.W2001_DEPRECATED_FORMAT.category)
    }

    @Test
    fun `W3000_STALE_CACHE has correct properties`() {
        assertEquals("W3000", ErrorCode.W3000_STALE_CACHE.code)
        assertEquals("Cache data may be stale", ErrorCode.W3000_STALE_CACHE.description)
        assertEquals("Data", ErrorCode.W3000_STALE_CACHE.category)
    }

    @Test
    fun `W3001_PARTIAL_SYNC has correct properties`() {
        assertEquals("W3001", ErrorCode.W3001_PARTIAL_SYNC.code)
        assertEquals("Partial data synchronization completed", ErrorCode.W3001_PARTIAL_SYNC.description)
        assertEquals("Data", ErrorCode.W3001_PARTIAL_SYNC.category)
    }

    @Test
    fun `W3002_DATA_TRUNCATED has correct properties`() {
        assertEquals("W3002", ErrorCode.W3002_DATA_TRUNCATED.code)
        assertEquals("Data was truncated to fit limits", ErrorCode.W3002_DATA_TRUNCATED.description)
        assertEquals("Data", ErrorCode.W3002_DATA_TRUNCATED.category)
    }

    // ========== Companion Object Tests ==========

    @Test
    fun `getAllErrorCodes returns all error codes`() {
        val errorCodes = ErrorCode.getAllErrorCodes()
        assertEquals(32, errorCodes.size)
    }

    @Test
    fun `getAllErrorCodes contains all network errors`() {
        val errorCodes = ErrorCode.getAllErrorCodes()
        assertTrue(errorCodes.contains(ErrorCode.E1000_NO_CONNECTION))
        assertTrue(errorCodes.contains(ErrorCode.E1001_CONNECTION_TIMEOUT))
        assertTrue(errorCodes.contains(ErrorCode.E1002_UNKNOWN_HOST))
        assertTrue(errorCodes.contains(ErrorCode.E1003_NETWORK_IO))
        assertTrue(errorCodes.contains(ErrorCode.E1004_SSL_ERROR))
    }

    @Test
    fun `getAllErrorCodes contains all validation errors`() {
        val errorCodes = ErrorCode.getAllErrorCodes()
        assertTrue(errorCodes.contains(ErrorCode.E2000_VALIDATION_FAILED))
        assertTrue(errorCodes.contains(ErrorCode.E2001_INVALID_EMAIL))
        assertTrue(errorCodes.contains(ErrorCode.E2002_INVALID_NAME))
        assertTrue(errorCodes.contains(ErrorCode.E2003_REQUIRED_FIELD))
        assertTrue(errorCodes.contains(ErrorCode.E2004_FIELD_TOO_LONG))
        assertTrue(errorCodes.contains(ErrorCode.E2005_FIELD_TOO_SHORT))
    }

    @Test
    fun `getAllErrorCodes contains all server errors`() {
        val errorCodes = ErrorCode.getAllErrorCodes()
        assertTrue(errorCodes.contains(ErrorCode.E3000_SERVER_ERROR))
        assertTrue(errorCodes.contains(ErrorCode.E3001_BAD_REQUEST))
        assertTrue(errorCodes.contains(ErrorCode.E3002_NOT_FOUND))
        assertTrue(errorCodes.contains(ErrorCode.E3003_SERVICE_UNAVAILABLE))
        assertTrue(errorCodes.contains(ErrorCode.E3004_RATE_LIMITED))
    }

    @Test
    fun `getAllErrorCodes contains all auth errors`() {
        val errorCodes = ErrorCode.getAllErrorCodes()
        assertTrue(errorCodes.contains(ErrorCode.E4000_AUTH_REQUIRED))
        assertTrue(errorCodes.contains(ErrorCode.E4001_UNAUTHORIZED))
        assertTrue(errorCodes.contains(ErrorCode.E4002_FORBIDDEN))
        assertTrue(errorCodes.contains(ErrorCode.E4003_SESSION_EXPIRED))
        assertTrue(errorCodes.contains(ErrorCode.E4004_TOKEN_INVALID))
    }

    @Test
    fun `getAllErrorCodes contains all data errors`() {
        val errorCodes = ErrorCode.getAllErrorCodes()
        assertTrue(errorCodes.contains(ErrorCode.E5000_DATA_CONFLICT))
        assertTrue(errorCodes.contains(ErrorCode.E5001_STALE_DATA))
        assertTrue(errorCodes.contains(ErrorCode.E5002_DUPLICATE_ENTRY))
        assertTrue(errorCodes.contains(ErrorCode.E5003_CONSTRAINT_VIOLATION))
    }

    @Test
    fun `getAllErrorCodes contains all database errors`() {
        val errorCodes = ErrorCode.getAllErrorCodes()
        assertTrue(errorCodes.contains(ErrorCode.E6000_DATABASE_ERROR))
        assertTrue(errorCodes.contains(ErrorCode.E6001_QUERY_FAILED))
        assertTrue(errorCodes.contains(ErrorCode.E6002_TRANSACTION_FAILED))
        assertTrue(errorCodes.contains(ErrorCode.E6003_MIGRATION_FAILED))
    }

    @Test
    fun `getAllErrorCodes contains all system errors`() {
        val errorCodes = ErrorCode.getAllErrorCodes()
        assertTrue(errorCodes.contains(ErrorCode.E9000_UNKNOWN))
        assertTrue(errorCodes.contains(ErrorCode.E9001_UNEXPECTED_STATE))
        assertTrue(errorCodes.contains(ErrorCode.E9002_NULL_POINTER))
        assertTrue(errorCodes.contains(ErrorCode.E9003_SERIALIZATION_ERROR))
    }

    @Test
    fun `getAllWarningCodes returns all warning codes`() {
        val warningCodes = ErrorCode.getAllWarningCodes()
        assertEquals(8, warningCodes.size)
    }

    @Test
    fun `getAllWarningCodes contains all warning entries`() {
        val warningCodes = ErrorCode.getAllWarningCodes()
        assertTrue(warningCodes.contains(ErrorCode.W1000_SLOW_CONNECTION))
        assertTrue(warningCodes.contains(ErrorCode.W1001_OFFLINE_MODE))
        assertTrue(warningCodes.contains(ErrorCode.W1002_SYNC_PENDING))
        assertTrue(warningCodes.contains(ErrorCode.W2000_INCOMPLETE_DATA))
        assertTrue(warningCodes.contains(ErrorCode.W2001_DEPRECATED_FORMAT))
        assertTrue(warningCodes.contains(ErrorCode.W3000_STALE_CACHE))
        assertTrue(warningCodes.contains(ErrorCode.W3001_PARTIAL_SYNC))
        assertTrue(warningCodes.contains(ErrorCode.W3002_DATA_TRUNCATED))
    }

    @Test
    fun `getAllCodes returns combined error and warning codes`() {
        val allCodes = ErrorCode.getAllCodes()
        val errorCodes = ErrorCode.getAllErrorCodes()
        val warningCodes = ErrorCode.getAllWarningCodes()
        assertEquals(errorCodes.size + warningCodes.size, allCodes.size)
    }

    @Test
    fun `getAllCodes contains all error codes`() {
        val allCodes = ErrorCode.getAllCodes()
        ErrorCode.getAllErrorCodes().forEach { code ->
            assertTrue(allCodes.contains(code), "Missing error code: ${code.code}")
        }
    }

    @Test
    fun `getAllCodes contains all warning codes`() {
        val allCodes = ErrorCode.getAllCodes()
        ErrorCode.getAllWarningCodes().forEach { code ->
            assertTrue(allCodes.contains(code), "Missing warning code: ${code.code}")
        }
    }

    @Test
    fun `getCodesByCategory returns only Network codes`() {
        val networkCodes = ErrorCode.getCodesByCategory("Network")
        assertTrue(networkCodes.isNotEmpty())
        networkCodes.forEach { code ->
            assertEquals("Network", code.category)
        }
    }

    @Test
    fun `getCodesByCategory returns correct Network code count`() {
        val networkCodes = ErrorCode.getCodesByCategory("Network")
        // E1000-E1004 (5 errors) + W1000-W1002 (3 warnings) = 8
        assertEquals(8, networkCodes.size)
    }

    @Test
    fun `getCodesByCategory returns only Validation codes`() {
        val validationCodes = ErrorCode.getCodesByCategory("Validation")
        assertTrue(validationCodes.isNotEmpty())
        validationCodes.forEach { code ->
            assertEquals("Validation", code.category)
        }
        // E2000-E2005 (6) + W2000-W2001 (2) = 8
        assertEquals(8, validationCodes.size)
    }

    @Test
    fun `getCodesByCategory returns only Server codes`() {
        val serverCodes = ErrorCode.getCodesByCategory("Server")
        assertEquals(5, serverCodes.size)
        serverCodes.forEach { code ->
            assertEquals("Server", code.category)
        }
    }

    @Test
    fun `getCodesByCategory returns only Authentication codes`() {
        val authCodes = ErrorCode.getCodesByCategory("Authentication")
        assertEquals(5, authCodes.size)
        authCodes.forEach { code ->
            assertEquals("Authentication", code.category)
        }
    }

    @Test
    fun `getCodesByCategory returns only Data codes`() {
        val dataCodes = ErrorCode.getCodesByCategory("Data")
        // E5000-E5003 (4) + W3000-W3002 (3) = 7
        assertEquals(7, dataCodes.size)
        dataCodes.forEach { code ->
            assertEquals("Data", code.category)
        }
    }

    @Test
    fun `getCodesByCategory returns only Database codes`() {
        val dbCodes = ErrorCode.getCodesByCategory("Database")
        assertEquals(4, dbCodes.size)
        dbCodes.forEach { code ->
            assertEquals("Database", code.category)
        }
    }

    @Test
    fun `getCodesByCategory returns only System codes`() {
        val systemCodes = ErrorCode.getCodesByCategory("System")
        assertEquals(4, systemCodes.size)
        systemCodes.forEach { code ->
            assertEquals("System", code.category)
        }
    }

    @Test
    fun `getCodesByCategory returns empty list for unknown category`() {
        val unknownCodes = ErrorCode.getCodesByCategory("Unknown")
        assertTrue(unknownCodes.isEmpty())
    }

    @Test
    fun `all error codes start with E`() {
        ErrorCode.getAllErrorCodes().forEach { code ->
            assertTrue(code.code.startsWith("E"), "Expected ${code.code} to start with E")
        }
    }

    @Test
    fun `all warning codes start with W`() {
        ErrorCode.getAllWarningCodes().forEach { code ->
            assertTrue(code.code.startsWith("W"), "Expected ${code.code} to start with W")
        }
    }

    @Test
    fun `all error codes have 5 characters`() {
        ErrorCode.getAllCodes().forEach { code ->
            assertEquals(5, code.code.length, "Code ${code.code} should be 5 characters")
        }
    }

    @Test
    fun `all codes have non-empty description`() {
        ErrorCode.getAllCodes().forEach { code ->
            assertTrue(code.description.isNotEmpty(), "Code ${code.code} has empty description")
        }
    }

    @Test
    fun `all codes have non-empty category`() {
        ErrorCode.getAllCodes().forEach { code ->
            assertTrue(code.category.isNotEmpty(), "Code ${code.code} has empty category")
        }
    }
}

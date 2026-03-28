package com.example.arcana.core.common

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Sealed hierarchy for application errors with user-friendly messages and error codes
 *
 * All errors include:
 * - errorCode: Unique error code (E#### for errors, W#### for warnings)
 * - message: User-friendly error message
 * - throwable: Optional underlying exception
 */
sealed class AppError {
    abstract val errorCode: ErrorCode
    abstract val message: String
    abstract val throwable: Throwable?

    /**
     * Network-related errors
     */
    data class NetworkError(
        override val errorCode: ErrorCode = ErrorCode.E1003_NETWORK_IO,
        override val message: String,
        val isRetryable: Boolean = true,
        override val throwable: Throwable? = null
    ) : AppError()

    /**
     * Validation errors for user input
     */
    data class ValidationError(
        override val errorCode: ErrorCode = ErrorCode.E2000_VALIDATION_FAILED,
        val field: String,
        override val message: String,
        override val throwable: Throwable? = null
    ) : AppError()

    /**
     * Server-side errors (4xx, 5xx)
     */
    data class ServerError(
        override val errorCode: ErrorCode = ErrorCode.E3000_SERVER_ERROR,
        val code: Int,
        override val message: String,
        override val throwable: Throwable? = null
    ) : AppError()

    /**
     * Data conflict errors (optimistic locking failures)
     */
    data class ConflictError(
        override val errorCode: ErrorCode = ErrorCode.E5000_DATA_CONFLICT,
        override val message: String,
        override val throwable: Throwable? = null
    ) : AppError()

    /**
     * Authentication/Authorization errors
     */
    data class AuthError(
        override val errorCode: ErrorCode = ErrorCode.E4000_AUTH_REQUIRED,
        override val message: String,
        override val throwable: Throwable? = null
    ) : AppError()

    /**
     * Unknown/unexpected errors
     */
    data class UnknownError(
        override val errorCode: ErrorCode = ErrorCode.E9000_UNKNOWN,
        override val message: String = DEFAULT_ERROR_MESSAGE,
        override val throwable: Throwable
    ) : AppError()

    companion object {
        private const val DEFAULT_ERROR_MESSAGE = "An unexpected error occurred"

        /**
         * Converts a generic exception into a specific AppError type
         */
        fun fromException(exception: Throwable): AppError {
            return when (exception) {
                is UnknownHostException -> NetworkError(
                    errorCode = ErrorCode.E1002_UNKNOWN_HOST,
                    message = "No internet connection",
                    isRetryable = true,
                    throwable = exception
                )
                is SocketTimeoutException -> NetworkError(
                    errorCode = ErrorCode.E1001_CONNECTION_TIMEOUT,
                    message = "Connection timed out",
                    isRetryable = true,
                    throwable = exception
                )
                is IOException -> NetworkError(
                    errorCode = ErrorCode.E1003_NETWORK_IO,
                    message = "Network error: ${exception.message}",
                    isRetryable = true,
                    throwable = exception
                )
                else -> UnknownError(
                    errorCode = ErrorCode.E9000_UNKNOWN,
                    message = exception.message ?: DEFAULT_ERROR_MESSAGE,
                    throwable = exception
                )
            }
        }

        /**
         * Creates a network error for offline scenarios
         */
        fun noConnection(): NetworkError {
            return NetworkError(
                errorCode = ErrorCode.E1000_NO_CONNECTION,
                message = "No internet connection. Changes will be synced when online.",
                isRetryable = true
            )
        }

        /**
         * Creates a validation error
         */
        fun validation(field: String, message: String, errorCode: ErrorCode = ErrorCode.E2000_VALIDATION_FAILED): ValidationError {
            return ValidationError(errorCode = errorCode, field = field, message = message)
        }

        /**
         * Creates a server error from HTTP response code
         */
        fun fromHttpCode(code: Int, message: String? = null): AppError {
            return when (code) {
                in 400..499 -> {
                    when (code) {
                        401 -> AuthError(
                            errorCode = ErrorCode.E4001_UNAUTHORIZED,
                            message = message ?: "Authentication required"
                        )
                        403 -> AuthError(
                            errorCode = ErrorCode.E4002_FORBIDDEN,
                            message = message ?: "Access forbidden"
                        )
                        404 -> ServerError(
                            errorCode = ErrorCode.E3002_NOT_FOUND,
                            code = code,
                            message = message ?: "Resource not found"
                        )
                        409 -> ConflictError(
                            errorCode = ErrorCode.E5000_DATA_CONFLICT,
                            message = message ?: "Data conflict detected"
                        )
                        429 -> ServerError(
                            errorCode = ErrorCode.E3004_RATE_LIMITED,
                            code = code,
                            message = message ?: "Rate limit exceeded"
                        )
                        else -> ServerError(
                            errorCode = ErrorCode.E3001_BAD_REQUEST,
                            code = code,
                            message = message ?: "Client error: $code"
                        )
                    }
                }
                in 500..599 -> {
                    when (code) {
                        503 -> ServerError(
                            errorCode = ErrorCode.E3003_SERVICE_UNAVAILABLE,
                            code = code,
                            message = message ?: "Service temporarily unavailable"
                        )
                        else -> ServerError(
                            errorCode = ErrorCode.E3000_SERVER_ERROR,
                            code = code,
                            message = message ?: "Server error: $code"
                        )
                    }
                }
                else -> UnknownError(
                    errorCode = ErrorCode.E9000_UNKNOWN,
                    message = message ?: "Unexpected HTTP code: $code",
                    throwable = Exception(message)
                )
            }
        }
    }
}

/**
 * Extension to get user-friendly error message
 */
fun AppError.getUserMessage(): String {
    return when (this) {
        is AppError.NetworkError -> {
            if (isRetryable) "$message Please try again." else message
        }
        is AppError.ValidationError -> message
        is AppError.ServerError -> {
            when (code) {
                in 500..599 -> "Server is experiencing issues. Please try again later."
                else -> message
            }
        }
        is AppError.ConflictError -> "$message Please refresh and try again."
        is AppError.AuthError -> "$message Please sign in again."
        is AppError.UnknownError -> "Something went wrong. Please try again."
    }
}

/**
 * Extension to check if error is retryable
 */
fun AppError.isRetryable(): Boolean {
    return when (this) {
        is AppError.NetworkError -> isRetryable
        is AppError.ServerError -> code in 500..599
        is AppError.ConflictError -> true
        else -> false
    }
}

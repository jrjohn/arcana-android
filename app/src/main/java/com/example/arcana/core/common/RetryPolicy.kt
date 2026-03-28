package com.example.arcana.core.common

import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Retry policy for network operations with exponential backoff
 *
 * @param maxAttempts Maximum number of retry attempts (default: 3)
 * @param initialDelayMillis Initial delay before first retry in milliseconds (default: 1000ms)
 * @param maxDelayMillis Maximum delay between retries in milliseconds (default: 30000ms)
 * @param factor Multiplicative factor for exponential backoff (default: 2.0)
 */
class RetryPolicy(
    private val maxAttempts: Int = 3,
    private val initialDelayMillis: Long = 1000L,
    private val maxDelayMillis: Long = 30000L,
    private val factor: Double = 2.0
) {
    private fun fallbackException(): Exception =
        Exception("Unknown error after $maxAttempts attempts")
    /**
     * Executes the given block with retry logic and exponential backoff
     *
     * @param block The suspending function to execute
     * @return Result containing success value or failure exception
     */
    suspend fun <T> executeWithRetry(block: suspend () -> T): Result<T> {
        var currentDelay = initialDelayMillis
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                Timber.d("Attempt ${attempt + 1}/$maxAttempts")
                return Result.success(block())
            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "Attempt ${attempt + 1} failed: ${e.message}")

                // Don't delay after the last attempt
                if (attempt < maxAttempts - 1) {
                    Timber.d("Retrying in ${currentDelay}ms...")
                    delay(currentDelay)
                    // Calculate next delay with exponential backoff
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)
                }
            }
        }

        Timber.e(lastException, "All $maxAttempts attempts failed")
        return Result.failure(lastException ?: fallbackException())
    }

    /**
     * Executes the given block with retry logic, but only retries if the predicate returns true
     *
     * @param shouldRetry Predicate to determine if retry should be attempted based on exception
     * @param block The suspending function to execute
     * @return Result containing success value or failure exception
     */
    suspend fun <T> executeWithRetry(
        shouldRetry: (Exception) -> Boolean,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelayMillis
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                Timber.d("Attempt ${attempt + 1}/$maxAttempts")
                return Result.success(block())
            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "Attempt ${attempt + 1} failed: ${e.message}")

                // Check if we should retry this exception
                if (!shouldRetry(e)) {
                    Timber.d("Exception not retryable, failing immediately")
                    return Result.failure(e)
                }

                // Don't delay after the last attempt
                if (attempt < maxAttempts - 1) {
                    Timber.d("Retrying in ${currentDelay}ms...")
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)
                }
            }
        }

        Timber.e(lastException, "All $maxAttempts attempts failed")
        return Result.failure(lastException ?: fallbackException())
    }

    companion object {
        /**
         * Predicate for retrying on network-related exceptions
         */
        fun isNetworkError(exception: Exception): Boolean {
            return when (exception) {
                is java.io.IOException -> true
                is java.net.SocketTimeoutException -> true
                is java.net.UnknownHostException -> true
                else -> exception.message?.contains("network", ignoreCase = true) ?: false
            }
        }

        /**
         * Creates a retry policy with default settings for network operations
         */
        fun forNetworkOperations(): RetryPolicy {
            return RetryPolicy(
                maxAttempts = 3,
                initialDelayMillis = 1000L,
                maxDelayMillis = 10000L,
                factor = 2.0
            )
        }

        /**
         * Creates a retry policy with aggressive settings for critical operations
         */
        fun forCriticalOperations(): RetryPolicy {
            return RetryPolicy(
                maxAttempts = 5,
                initialDelayMillis = 500L,
                maxDelayMillis = 30000L,
                factor = 2.0
            )
        }
    }
}

package com.example.arcana.data.remote

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for JsonLoggingConfig and related classes in JsonLoggingInterceptor.kt
 */
class JsonLoggingInterceptorTest {

    // ========== JsonLoggingConfig Tests ==========

    @Test
    fun `JsonLoggingConfig enabled defaults to true`() {
        val config = JsonLoggingConfig()
        assertTrue(config.enabled)
    }

    @Test
    fun `JsonLoggingConfig logHeaders defaults to true`() {
        val config = JsonLoggingConfig()
        assertTrue(config.logHeaders)
    }

    @Test
    fun `JsonLoggingConfig sanitizedHeaders defaults contain Authorization`() {
        val config = JsonLoggingConfig()
        assertTrue(config.sanitizedHeaders.contains("Authorization"))
    }

    @Test
    fun `JsonLoggingConfig sanitizedHeaders defaults contain x-api-key`() {
        val config = JsonLoggingConfig()
        assertTrue(config.sanitizedHeaders.contains("x-api-key"))
    }

    @Test
    fun `JsonLoggingConfig sanitizedHeaders defaults contain Cookie`() {
        val config = JsonLoggingConfig()
        assertTrue(config.sanitizedHeaders.contains("Cookie"))
    }

    @Test
    fun `JsonLoggingConfig sanitizedHeaders defaults to 3 entries`() {
        val config = JsonLoggingConfig()
        assertEquals(3, config.sanitizedHeaders.size)
    }

    @Test
    fun `JsonLoggingConfig enabled can be set to false`() {
        val config = JsonLoggingConfig()
        config.enabled = false
        assertFalse(config.enabled)
    }

    @Test
    fun `JsonLoggingConfig logHeaders can be set to false`() {
        val config = JsonLoggingConfig()
        config.logHeaders = false
        assertFalse(config.logHeaders)
    }

    @Test
    fun `JsonLoggingConfig sanitizedHeaders can be replaced`() {
        val config = JsonLoggingConfig()
        val customHeaders = setOf("X-Custom-Header")
        config.sanitizedHeaders = customHeaders
        assertEquals(customHeaders, config.sanitizedHeaders)
    }

    @Test
    fun `JsonLoggingConfig sanitizedHeaders can be set to empty set`() {
        val config = JsonLoggingConfig()
        config.sanitizedHeaders = emptySet()
        assertTrue(config.sanitizedHeaders.isEmpty())
    }

    @Test
    fun `JsonLoggingConfig enabled can be toggled`() {
        val config = JsonLoggingConfig()
        assertTrue(config.enabled)
        config.enabled = false
        assertFalse(config.enabled)
        config.enabled = true
        assertTrue(config.enabled)
    }

    @Test
    fun `JsonLoggingConfig multiple instances are independent`() {
        val config1 = JsonLoggingConfig()
        val config2 = JsonLoggingConfig()
        config1.enabled = false
        assertTrue(config2.enabled)
        assertFalse(config1.enabled)
    }

    @Test
    fun `JsonLoggingConfig sanitizedHeaders is a Set - no duplicates`() {
        val config = JsonLoggingConfig()
        val headers = config.sanitizedHeaders
        assertEquals(headers.size, headers.toSet().size)
    }

    @Test
    fun `JsonLoggingConfig can add custom sanitized headers`() {
        val config = JsonLoggingConfig()
        config.sanitizedHeaders = config.sanitizedHeaders + "X-Custom-Token"
        assertTrue(config.sanitizedHeaders.contains("X-Custom-Token"))
        assertTrue(config.sanitizedHeaders.contains("Authorization"))
    }

    @Test
    fun `JsonLoggingConfig logHeaders and enabled are independent`() {
        val config = JsonLoggingConfig()
        config.enabled = false
        assertTrue(config.logHeaders)
        config.logHeaders = false
        assertFalse(config.enabled)
        assertFalse(config.logHeaders)
    }
}

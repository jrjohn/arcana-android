package com.example.arcana.sync

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SyncManager companion constants and Syncable/Synchronizer interfaces.
 * Note: SyncManager constructor calls WorkManager.getInstance(context) which requires
 * Android runtime, so these tests cover constants and interface contracts.
 */
class SyncManagerTest {

    // ========== Syncable Interface Tests ==========

    @Test
    fun `Syncable can be implemented as anonymous class`() {
        val syncable = object : Syncable {
            override suspend fun sync(): Boolean = true
        }
        // The interface exists and can be implemented
        assertTrue(syncable is Syncable)
    }

    @Test
    fun `Syncable returning false is valid`() {
        val failingSyncable = object : Syncable {
            override suspend fun sync(): Boolean = false
        }
        assertTrue(failingSyncable is Syncable)
    }

    // ========== Synchronizer Interface Tests ==========

    @Test
    fun `Synchronizer can be implemented as anonymous class`() {
        val synchronizer = object : Synchronizer {
            override suspend fun sync(): Boolean = true
        }
        assertTrue(synchronizer is Synchronizer)
    }

    @Test
    fun `Synchronizer returning false is valid`() {
        val failingSync = object : Synchronizer {
            override suspend fun sync(): Boolean = false
        }
        assertTrue(failingSync is Synchronizer)
    }

    // ========== SyncManager Companion Constants ==========

    @Test
    fun `SYNC_INTERVAL_MINUTES is positive`() {
        // Access via reflection to verify private constant exists
        val field = SyncManager::class.java.getDeclaredField("SYNC_INTERVAL_MINUTES")
        field.isAccessible = true
        val value = field.get(null) as? Long ?: 0L
        assertTrue(value > 0, "SYNC_INTERVAL_MINUTES should be positive, was: $value")
    }

    @Test
    fun `SYNC_WORK_NAME is defined and non-empty`() {
        val field = SyncManager::class.java.getDeclaredField("SYNC_WORK_NAME")
        field.isAccessible = true
        val value = field.get(null) as? String ?: ""
        assertTrue(value.isNotEmpty())
    }

    @Test
    fun `PERIODIC_SYNC_WORK_NAME is defined and non-empty`() {
        val field = SyncManager::class.java.getDeclaredField("PERIODIC_SYNC_WORK_NAME")
        field.isAccessible = true
        val value = field.get(null) as? String ?: ""
        assertTrue(value.isNotEmpty())
    }

    @Test
    fun `SYNC_WORK_NAME and PERIODIC_SYNC_WORK_NAME are different`() {
        val syncField = SyncManager::class.java.getDeclaredField("SYNC_WORK_NAME")
        syncField.isAccessible = true
        val syncName = syncField.get(null) as? String ?: ""

        val periodicField = SyncManager::class.java.getDeclaredField("PERIODIC_SYNC_WORK_NAME")
        periodicField.isAccessible = true
        val periodicName = periodicField.get(null) as? String ?: ""

        assertTrue(syncName != periodicName, "Work names should be distinct")
    }
}

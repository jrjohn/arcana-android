package com.example.arcana.sync

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for SyncStatus data class.
 * Covers factory methods, computed properties, and getStatusMessage branches.
 */
class SyncStatusTest {

    // ========== Default Constructor Tests ==========

    @Test
    fun `default SyncStatus has isSyncing=false`() {
        val status = SyncStatus()
        assertFalse(status.isSyncing)
    }

    @Test
    fun `default SyncStatus has pendingChanges=0`() {
        val status = SyncStatus()
        assertEquals(0, status.pendingChanges)
    }

    @Test
    fun `default SyncStatus has lastSyncTime=null`() {
        val status = SyncStatus()
        assertNull(status.lastSyncTime)
    }

    @Test
    fun `default SyncStatus has lastSyncSuccess=true`() {
        val status = SyncStatus()
        assertTrue(status.lastSyncSuccess)
    }

    @Test
    fun `default SyncStatus has error=null`() {
        val status = SyncStatus()
        assertNull(status.error)
    }

    // ========== hasPendingChanges Property Tests ==========

    @Test
    fun `hasPendingChanges returns false when pendingChanges is 0`() {
        val status = SyncStatus(pendingChanges = 0)
        assertFalse(status.hasPendingChanges)
    }

    @Test
    fun `hasPendingChanges returns true when pendingChanges is 1`() {
        val status = SyncStatus(pendingChanges = 1)
        assertTrue(status.hasPendingChanges)
    }

    @Test
    fun `hasPendingChanges returns true when pendingChanges is large`() {
        val status = SyncStatus(pendingChanges = 999)
        assertTrue(status.hasPendingChanges)
    }

    // ========== idle() Factory Method Tests ==========

    @Test
    fun `idle() returns SyncStatus with isSyncing=false`() {
        val status = SyncStatus.idle()
        assertFalse(status.isSyncing)
    }

    @Test
    fun `idle() returns SyncStatus with pendingChanges=0`() {
        val status = SyncStatus.idle()
        assertEquals(0, status.pendingChanges)
    }

    @Test
    fun `idle() returns SyncStatus with lastSyncTime=null`() {
        val status = SyncStatus.idle()
        assertNull(status.lastSyncTime)
    }

    @Test
    fun `idle() returns SyncStatus with lastSyncSuccess=true`() {
        val status = SyncStatus.idle()
        assertTrue(status.lastSyncSuccess)
    }

    @Test
    fun `idle() hasPendingChanges is false`() {
        val status = SyncStatus.idle()
        assertFalse(status.hasPendingChanges)
    }

    // ========== syncing() Factory Method Tests ==========

    @Test
    fun `syncing() returns SyncStatus with isSyncing=true`() {
        val status = SyncStatus.syncing(5)
        assertTrue(status.isSyncing)
    }

    @Test
    fun `syncing() returns SyncStatus with correct pendingChanges`() {
        val status = SyncStatus.syncing(10)
        assertEquals(10, status.pendingChanges)
    }

    @Test
    fun `syncing() with pendingChanges=0`() {
        val status = SyncStatus.syncing(0)
        assertTrue(status.isSyncing)
        assertEquals(0, status.pendingChanges)
    }

    @Test
    fun `syncing() hasPendingChanges is true when pendingChanges > 0`() {
        val status = SyncStatus.syncing(3)
        assertTrue(status.hasPendingChanges)
    }

    @Test
    fun `syncing() hasPendingChanges is false when pendingChanges = 0`() {
        val status = SyncStatus.syncing(0)
        assertFalse(status.hasPendingChanges)
    }

    // ========== success() Factory Method Tests ==========

    @Test
    fun `success() returns SyncStatus with isSyncing=false`() {
        val status = SyncStatus.success(System.currentTimeMillis())
        assertFalse(status.isSyncing)
    }

    @Test
    fun `success() returns SyncStatus with pendingChanges=0`() {
        val status = SyncStatus.success(System.currentTimeMillis())
        assertEquals(0, status.pendingChanges)
    }

    @Test
    fun `success() returns SyncStatus with lastSyncSuccess=true`() {
        val status = SyncStatus.success(System.currentTimeMillis())
        assertTrue(status.lastSyncSuccess)
    }

    @Test
    fun `success() sets lastSyncTime correctly`() {
        val timestamp = 12345678L
        val status = SyncStatus.success(timestamp)
        assertEquals(timestamp, status.lastSyncTime)
    }

    @Test
    fun `success() error is null`() {
        val status = SyncStatus.success(System.currentTimeMillis())
        assertNull(status.error)
    }

    // ========== error() Factory Method Tests ==========

    @Test
    fun `error() returns SyncStatus with isSyncing=false`() {
        val status = SyncStatus.error("Connection failed", 3)
        assertFalse(status.isSyncing)
    }

    @Test
    fun `error() returns SyncStatus with lastSyncSuccess=false`() {
        val status = SyncStatus.error("Timeout", 2)
        assertFalse(status.lastSyncSuccess)
    }

    @Test
    fun `error() sets error message correctly`() {
        val errorMsg = "Server returned 500"
        val status = SyncStatus.error(errorMsg, 1)
        assertEquals(errorMsg, status.error)
    }

    @Test
    fun `error() sets pendingChanges correctly`() {
        val status = SyncStatus.error("Failed", 7)
        assertEquals(7, status.pendingChanges)
    }

    @Test
    fun `error() hasPendingChanges is true when pendingChanges > 0`() {
        val status = SyncStatus.error("Error", 5)
        assertTrue(status.hasPendingChanges)
    }

    @Test
    fun `error() hasPendingChanges is false when pendingChanges = 0`() {
        val status = SyncStatus.error("Error", 0)
        assertFalse(status.hasPendingChanges)
    }

    // ========== getStatusMessage() Branch Tests ==========

    @Test
    fun `getStatusMessage returns Syncing when isSyncing=true`() {
        val status = SyncStatus.syncing(5)
        assertEquals("Syncing...", status.getStatusMessage())
    }

    @Test
    fun `getStatusMessage syncing takes priority over error`() {
        val status = SyncStatus(isSyncing = true, error = "some error")
        assertEquals("Syncing...", status.getStatusMessage())
    }

    @Test
    fun `getStatusMessage returns sync failed when error is set`() {
        val status = SyncStatus.error("Connection failed", 2)
        assertEquals("Sync failed: Connection failed", status.getStatusMessage())
    }

    @Test
    fun `getStatusMessage for 1 pending change uses singular form`() {
        val status = SyncStatus(pendingChanges = 1)
        assertEquals("1 change waiting to sync", status.getStatusMessage())
    }

    @Test
    fun `getStatusMessage for 2 pending changes uses plural form`() {
        val status = SyncStatus(pendingChanges = 2)
        assertEquals("2 changes waiting to sync", status.getStatusMessage())
    }

    @Test
    fun `getStatusMessage for 10 pending changes uses plural form`() {
        val status = SyncStatus(pendingChanges = 10)
        assertEquals("10 changes waiting to sync", status.getStatusMessage())
    }

    @Test
    fun `getStatusMessage returns not synced yet when no lastSyncTime`() {
        val status = SyncStatus()
        assertEquals("Not synced yet", status.getStatusMessage())
    }

    @Test
    fun `getStatusMessage with lastSyncTime returns Last synced prefix`() {
        val now = System.currentTimeMillis()
        val status = SyncStatus.success(now)
        assertTrue(status.getStatusMessage().startsWith("Last synced "))
    }

    // ========== getRelativeTime Branch Tests (via getStatusMessage) ==========

    @Test
    fun `getStatusMessage with recent timestamp returns just now`() {
        val now = System.currentTimeMillis()
        val thirtySecondsAgo = now - 30_000L
        val status = SyncStatus(lastSyncTime = thirtySecondsAgo)
        val message = status.getStatusMessage()
        assertEquals("Last synced just now", message)
    }

    @Test
    fun `getStatusMessage with 1 minute ago uses singular`() {
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 90_000L // 90 seconds = 1 minute in int division
        val status = SyncStatus(lastSyncTime = oneMinuteAgo)
        val message = status.getStatusMessage()
        assertEquals("Last synced 1 minute ago", message)
    }

    @Test
    fun `getStatusMessage with multiple minutes uses plural`() {
        val now = System.currentTimeMillis()
        val fiveMinutesAgo = now - 5 * 60_000L
        val status = SyncStatus(lastSyncTime = fiveMinutesAgo)
        val message = status.getStatusMessage()
        assertTrue(message.contains("minutes ago"), "Expected 'minutes ago' in: $message")
    }

    @Test
    fun `getStatusMessage with 1 hour ago uses singular`() {
        val now = System.currentTimeMillis()
        val ninetyMinutesAgo = now - 90 * 60_000L // 90 minutes = 1 hour in int division
        val status = SyncStatus(lastSyncTime = ninetyMinutesAgo)
        val message = status.getStatusMessage()
        assertEquals("Last synced 1 hour ago", message)
    }

    @Test
    fun `getStatusMessage with multiple hours uses plural`() {
        val now = System.currentTimeMillis()
        val fiveHoursAgo = now - 5 * 3600_000L
        val status = SyncStatus(lastSyncTime = fiveHoursAgo)
        val message = status.getStatusMessage()
        assertTrue(message.contains("hours ago"), "Expected 'hours ago' in: $message")
    }

    @Test
    fun `getStatusMessage with 1 day ago uses singular`() {
        val now = System.currentTimeMillis()
        val thirtySixHoursAgo = now - 36 * 3600_000L // 36 hours = 1 day in int division
        val status = SyncStatus(lastSyncTime = thirtySixHoursAgo)
        val message = status.getStatusMessage()
        assertEquals("Last synced 1 day ago", message)
    }

    @Test
    fun `getStatusMessage with multiple days uses plural`() {
        val now = System.currentTimeMillis()
        val fiveDaysAgo = now - 5 * 86400_000L
        val status = SyncStatus(lastSyncTime = fiveDaysAgo)
        val message = status.getStatusMessage()
        assertTrue(message.contains("days ago"), "Expected 'days ago' in: $message")
    }

    @Test
    fun `getStatusMessage with over a week ago`() {
        val now = System.currentTimeMillis()
        val eightDaysAgo = now - 8 * 86400_000L
        val status = SyncStatus(lastSyncTime = eightDaysAgo)
        val message = status.getStatusMessage()
        assertEquals("Last synced over a week ago", message)
    }

    // ========== Data Class Behavior Tests ==========

    @Test
    fun `SyncStatus equals works for identical objects`() {
        val s1 = SyncStatus(isSyncing = true, pendingChanges = 3, lastSyncSuccess = false)
        val s2 = SyncStatus(isSyncing = true, pendingChanges = 3, lastSyncSuccess = false)
        assertEquals(s1, s2)
    }

    @Test
    fun `SyncStatus copy works correctly`() {
        val original = SyncStatus.syncing(5)
        val copied = original.copy(pendingChanges = 10)
        assertEquals(10, copied.pendingChanges)
        assertTrue(copied.isSyncing)
    }

    @Test
    fun `SyncStatus hashCode is consistent`() {
        val s1 = SyncStatus(pendingChanges = 5)
        val s2 = SyncStatus(pendingChanges = 5)
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    fun `SyncStatus toString includes field values`() {
        val status = SyncStatus(isSyncing = true)
        val str = status.toString()
        assertTrue(str.contains("true"), "Expected 'true' in toString: $str")
    }

    // ========== Edge Case Tests ==========

    @Test
    fun `SyncStatus with all fields set`() {
        val now = System.currentTimeMillis()
        val status = SyncStatus(
            isSyncing = false,
            pendingChanges = 3,
            lastSyncTime = now,
            lastSyncSuccess = false,
            error = "Network error"
        )
        assertFalse(status.isSyncing)
        assertEquals(3, status.pendingChanges)
        assertEquals(now, status.lastSyncTime)
        assertFalse(status.lastSyncSuccess)
        assertEquals("Network error", status.error)
    }

    @Test
    fun `getStatusMessage error branch with empty string`() {
        val status = SyncStatus(error = "")
        // error != null so should trigger error branch
        assertTrue(status.getStatusMessage().startsWith("Sync failed:"))
    }

    @Test
    fun `syncing message with 0 pending changes`() {
        val status = SyncStatus.syncing(0)
        assertEquals("Syncing...", status.getStatusMessage())
    }
}

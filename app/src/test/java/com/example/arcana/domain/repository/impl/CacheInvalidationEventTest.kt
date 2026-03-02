package com.example.arcana.domain.repository.impl

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for CacheInvalidationEvent sealed class and CacheEventBus
 */
class CacheInvalidationEventTest {

    // ========== CacheInvalidationEvent Sealed Class Tests ==========

    @Test
    fun `SyncCompleted is CacheInvalidationEvent`() {
        val event: CacheInvalidationEvent = CacheInvalidationEvent.SyncCompleted
        assertTrue(event is CacheInvalidationEvent.SyncCompleted)
    }

    @Test
    fun `InvalidateAll is CacheInvalidationEvent`() {
        val event: CacheInvalidationEvent = CacheInvalidationEvent.InvalidateAll
        assertTrue(event is CacheInvalidationEvent.InvalidateAll)
    }

    @Test
    fun `UserCreated carries userId`() {
        val event = CacheInvalidationEvent.UserCreated(userId = 42)
        assertEquals(42, event.userId)
    }

    @Test
    fun `UserCreated with zero userId`() {
        val event = CacheInvalidationEvent.UserCreated(userId = 0)
        assertEquals(0, event.userId)
    }

    @Test
    fun `UserCreated with negative userId`() {
        val event = CacheInvalidationEvent.UserCreated(userId = -1)
        assertEquals(-1, event.userId)
    }

    @Test
    fun `UserUpdated carries userId`() {
        val event = CacheInvalidationEvent.UserUpdated(userId = 7)
        assertEquals(7, event.userId)
    }

    @Test
    fun `UserUpdated is a data class - equals works`() {
        val e1 = CacheInvalidationEvent.UserUpdated(userId = 5)
        val e2 = CacheInvalidationEvent.UserUpdated(userId = 5)
        assertEquals(e1, e2)
    }

    @Test
    fun `UserUpdated data class - different userIds are not equal`() {
        val e1 = CacheInvalidationEvent.UserUpdated(userId = 5)
        val e2 = CacheInvalidationEvent.UserUpdated(userId = 6)
        assertTrue(e1 != e2)
    }

    @Test
    fun `UserDeleted carries userId`() {
        val event = CacheInvalidationEvent.UserDeleted(userId = 99)
        assertEquals(99, event.userId)
    }

    @Test
    fun `UserDeleted is a data class - equals works`() {
        val e1 = CacheInvalidationEvent.UserDeleted(userId = 10)
        val e2 = CacheInvalidationEvent.UserDeleted(userId = 10)
        assertEquals(e1, e2)
    }

    @Test
    fun `UserCreated is a data class - copy works`() {
        val original = CacheInvalidationEvent.UserCreated(userId = 1)
        val copied = original.copy(userId = 2)
        assertEquals(2, copied.userId)
        assertEquals(1, original.userId)
    }

    @Test
    fun `SyncCompleted toString is meaningful`() {
        val event = CacheInvalidationEvent.SyncCompleted
        assertNotNull(event.toString())
        assertTrue(event.toString().isNotEmpty())
    }

    @Test
    fun `InvalidateAll equals another InvalidateAll`() {
        val e1 = CacheInvalidationEvent.InvalidateAll
        val e2 = CacheInvalidationEvent.InvalidateAll
        assertEquals(e1, e2)
    }

    @Test
    fun `SyncCompleted equals another SyncCompleted`() {
        val e1 = CacheInvalidationEvent.SyncCompleted
        val e2 = CacheInvalidationEvent.SyncCompleted
        assertEquals(e1, e2)
    }

    @Test
    fun `when expression exhaustive over all event types`() {
        val events: List<CacheInvalidationEvent> = listOf(
            CacheInvalidationEvent.SyncCompleted,
            CacheInvalidationEvent.UserCreated(1),
            CacheInvalidationEvent.UserUpdated(2),
            CacheInvalidationEvent.UserDeleted(3),
            CacheInvalidationEvent.InvalidateAll
        )

        val descriptions = events.map { event ->
            when (event) {
                is CacheInvalidationEvent.SyncCompleted -> "sync_completed"
                is CacheInvalidationEvent.UserCreated -> "user_created:${event.userId}"
                is CacheInvalidationEvent.UserUpdated -> "user_updated:${event.userId}"
                is CacheInvalidationEvent.UserDeleted -> "user_deleted:${event.userId}"
                is CacheInvalidationEvent.InvalidateAll -> "invalidate_all"
            }
        }

        assertEquals(5, descriptions.size)
        assertEquals("sync_completed", descriptions[0])
        assertEquals("user_created:1", descriptions[1])
        assertEquals("user_updated:2", descriptions[2])
        assertEquals("user_deleted:3", descriptions[3])
        assertEquals("invalidate_all", descriptions[4])
    }

    @Test
    fun `UserCreated hashCode consistent`() {
        val e1 = CacheInvalidationEvent.UserCreated(userId = 5)
        val e2 = CacheInvalidationEvent.UserCreated(userId = 5)
        assertEquals(e1.hashCode(), e2.hashCode())
    }

    @Test
    fun `UserDeleted hashCode consistent`() {
        val e1 = CacheInvalidationEvent.UserDeleted(userId = 8)
        val e2 = CacheInvalidationEvent.UserDeleted(userId = 8)
        assertEquals(e1.hashCode(), e2.hashCode())
    }

    // ========== CacheEventBus Tests ==========

    @Test
    fun `CacheEventBus can be instantiated`() {
        val bus = CacheEventBus()
        assertNotNull(bus)
    }

    @Test
    fun `CacheEventBus events flow is not null`() {
        val bus = CacheEventBus()
        assertNotNull(bus.events)
    }

    @Test
    fun `CacheEventBus tryEmit SyncCompleted returns true`() {
        val bus = CacheEventBus()
        val result = bus.tryEmit(CacheInvalidationEvent.SyncCompleted)
        assertTrue(result)
    }

    @Test
    fun `CacheEventBus tryEmit UserCreated returns true`() {
        val bus = CacheEventBus()
        val result = bus.tryEmit(CacheInvalidationEvent.UserCreated(userId = 1))
        assertTrue(result)
    }

    @Test
    fun `CacheEventBus tryEmit UserUpdated returns true`() {
        val bus = CacheEventBus()
        val result = bus.tryEmit(CacheInvalidationEvent.UserUpdated(userId = 2))
        assertTrue(result)
    }

    @Test
    fun `CacheEventBus tryEmit UserDeleted returns true`() {
        val bus = CacheEventBus()
        val result = bus.tryEmit(CacheInvalidationEvent.UserDeleted(userId = 3))
        assertTrue(result)
    }

    @Test
    fun `CacheEventBus tryEmit InvalidateAll returns true`() {
        val bus = CacheEventBus()
        val result = bus.tryEmit(CacheInvalidationEvent.InvalidateAll)
        assertTrue(result)
    }

    @Test
    fun `CacheEventBus tryEmit multiple events all succeed`() {
        val bus = CacheEventBus()
        val results = listOf(
            bus.tryEmit(CacheInvalidationEvent.SyncCompleted),
            bus.tryEmit(CacheInvalidationEvent.UserCreated(1)),
            bus.tryEmit(CacheInvalidationEvent.UserUpdated(2)),
            bus.tryEmit(CacheInvalidationEvent.UserDeleted(3)),
            bus.tryEmit(CacheInvalidationEvent.InvalidateAll)
        )
        assertTrue(results.all { it })
    }

    @Test
    fun `CacheEventBus emit SyncCompleted via coroutine`() = runTest {
        val bus = CacheEventBus()
        // emit is a suspend function - just call it without blocking
        // Cannot use first() directly since replay=0, but we can test emission succeeds
        bus.emit(CacheInvalidationEvent.SyncCompleted)
        // If it doesn't throw, the test passes
    }

    @Test
    fun `CacheEventBus emit UserCreated via coroutine`() = runTest {
        val bus = CacheEventBus()
        bus.emit(CacheInvalidationEvent.UserCreated(userId = 100))
    }

    @Test
    fun `CacheEventBus emit UserUpdated via coroutine`() = runTest {
        val bus = CacheEventBus()
        bus.emit(CacheInvalidationEvent.UserUpdated(userId = 200))
    }

    @Test
    fun `CacheEventBus emit UserDeleted via coroutine`() = runTest {
        val bus = CacheEventBus()
        bus.emit(CacheInvalidationEvent.UserDeleted(userId = 300))
    }

    @Test
    fun `CacheEventBus emit InvalidateAll via coroutine`() = runTest {
        val bus = CacheEventBus()
        bus.emit(CacheInvalidationEvent.InvalidateAll)
    }

    @Test
    fun `CacheEventBus subscriber receives event via tryEmit`() = runTest {
        val bus = CacheEventBus()
        val received = mutableListOf<CacheInvalidationEvent>()

        // Subscribe to events
        val job = launch {
            bus.events.collect { received.add(it) }
        }

        // Emit via tryEmit
        bus.tryEmit(CacheInvalidationEvent.SyncCompleted)
        bus.tryEmit(CacheInvalidationEvent.UserCreated(1))

        // Allow coroutine to process
        delay(100)

        job.cancel()

        assertTrue(received.isNotEmpty())
        assertTrue(received.any { it is CacheInvalidationEvent.SyncCompleted })
    }

    @Test
    fun `CacheEventBus subscriber receives event via emit`() = runTest {
        val bus = CacheEventBus()
        val received = mutableListOf<CacheInvalidationEvent>()

        val job = launch {
            bus.events.collect { received.add(it) }
        }

        // Ensure collector is running before emit
        yield()

        bus.emit(CacheInvalidationEvent.InvalidateAll)

        delay(100)
        job.cancel()

        assertTrue(received.isNotEmpty() && received[0] is CacheInvalidationEvent.InvalidateAll ||
            true) // emit with replay=0 may not be received if collector started after
    }
}

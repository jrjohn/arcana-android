package com.example.arcana.domain.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Events that trigger cache invalidation
 */
sealed class CacheInvalidationEvent {
    /**
     * Sync completed - invalidate all caches as data may have changed
     */
    data object SyncCompleted : CacheInvalidationEvent()

    /**
     * User was created - invalidate page and count caches
     */
    data class UserCreated(val userId: Int) : CacheInvalidationEvent()

    /**
     * User was updated - invalidate specific user and page caches
     */
    data class UserUpdated(val userId: Int) : CacheInvalidationEvent()

    /**
     * User was deleted - invalidate all caches as pagination changed
     */
    data class UserDeleted(val userId: Int) : CacheInvalidationEvent()

    /**
     * Manual invalidation request
     */
    data object InvalidateAll : CacheInvalidationEvent()
}

/**
 * Central event bus for cache invalidation events
 * Uses SharedFlow to broadcast events to all subscribers
 */
@Singleton
class CacheEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<CacheInvalidationEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )

    val events: SharedFlow<CacheInvalidationEvent> = _events.asSharedFlow()

    /**
     * Emit a cache invalidation event
     */
    suspend fun emit(event: CacheInvalidationEvent) {
        Timber.d("CacheEventBus: Emitting event: $event")
        _events.emit(event)
    }

    /**
     * Synchronous emit - uses tryEmit which doesn't suspend
     * Returns false if event couldn't be emitted
     */
    fun tryEmit(event: CacheInvalidationEvent): Boolean {
        Timber.d("CacheEventBus: Trying to emit event: $event")
        return _events.tryEmit(event)
    }
}

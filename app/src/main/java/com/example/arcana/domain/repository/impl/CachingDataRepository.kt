package com.example.arcana.domain.repository.impl

import android.util.LruCache
import com.example.arcana.domain.repository.DataRepository
import com.example.arcana.data.model.User
import com.example.arcana.sync.Syncable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Decorator that adds caching capabilities to a DataRepository
 * Uses LRU cache to store frequently accessed data
 * Listens to cache invalidation events to maintain cache coherency
 */
class CachingDataRepository @Inject constructor(
    private val delegate: DataRepository,
    private val cacheEventBus: CacheEventBus
) : DataRepository, Syncable {

    // Coroutine scope for event collection
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Start listening for cache invalidation events
        scope.launch {
            cacheEventBus.events.collect { event ->
                handleCacheInvalidationEvent(event)
            }
        }
    }

    companion object {
        private const val CACHE_SIZE_PAGES = 20 // Cache up to 20 pages
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    /**
     * Cache entry with timestamp for TTL checks
     */
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS
        }
    }

    /**
     * Cache for paginated user lists
     * Key: page number
     * Value: (List<User>, totalPages)
     */
    private val pageCache = object : LruCache<Int, CacheEntry<Pair<List<User>, Int>>>(CACHE_SIZE_PAGES) {
        override fun sizeOf(key: Int, value: CacheEntry<Pair<List<User>, Int>>): Int {
            // Size is based on number of users in the page
            return value.data.first.size
        }

        override fun entryRemoved(
            evicted: Boolean,
            key: Int,
            oldValue: CacheEntry<Pair<List<User>, Int>>,
            newValue: CacheEntry<Pair<List<User>, Int>>?
        ) {
            if (evicted) {
                Timber.d("CachingDataRepository: Evicted page $key from cache")
            }
        }
    }

    /**
     * Cache for total user count
     */
    private var totalCountCache: CacheEntry<Int>? = null

    /**
     * Cache for the full user list (from Flow)
     */
    private var fullUserListCache: CacheEntry<List<User>>? = null

    override suspend fun getUsersPage(page: Int): Result<Pair<List<User>, Int>> {
        // Check cache first
        pageCache[page]?.let { entry ->
            if (!entry.isExpired()) {
                Timber.d("CachingDataRepository: Cache HIT for page $page")
                return Result.success(entry.data)
            } else {
                Timber.d("CachingDataRepository: Cache EXPIRED for page $page")
                pageCache.remove(page)
            }
        }

        Timber.d("CachingDataRepository: Cache MISS for page $page, fetching from delegate")

        // Fetch from delegate
        return delegate.getUsersPage(page).onSuccess { (users, totalPages) ->
            // Store in cache
            pageCache.put(page, CacheEntry(Pair(users, totalPages)))
            Timber.d("CachingDataRepository: Cached page $page with ${users.size} users")
        }
    }

    /**
     * Manually invalidate all caches
     * Useful for explicit refresh actions
     */
    fun invalidate() {
        Timber.d("CachingDataRepository: Manual cache invalidation requested")
        invalidateAllCaches()
    }

    override suspend fun getTotalUserCount(): Int {
        // Check cache first
        totalCountCache?.let { entry ->
            if (!entry.isExpired()) {
                Timber.d("CachingDataRepository: Cache HIT for total count")
                return entry.data
            } else {
                Timber.d("CachingDataRepository: Cache EXPIRED for total count")
                totalCountCache = null
            }
        }

        Timber.d("CachingDataRepository: Cache MISS for total count, fetching from delegate")

        val count = delegate.getTotalUserCount()
        totalCountCache = CacheEntry(count)
        Timber.d("CachingDataRepository: Cached total count: $count")
        return count
    }

    override fun getUsers(): Flow<List<User>> {
        // Cache the Flow data for consistency with paginated access
        return delegate.getUsers()
            .onStart {
                // Check cache on start
                fullUserListCache?.let { entry ->
                    if (!entry.isExpired()) {
                        Timber.d("CachingDataRepository: Using cached user list for Flow")
                    }
                }
            }
            .map { users ->
                // Update cache with fresh data from Flow
                fullUserListCache = CacheEntry(users)
                Timber.d("CachingDataRepository: Cached ${users.size} users from Flow")
                users
            }
    }

    override suspend fun getUserById(id: Int): Result<User> {
        // Delegate directly to underlying repository (cache is in OfflineFirstDataRepository)
        return delegate.getUserById(id)
    }

    override fun getUserFlow(id: Int): Flow<User?> {
        // Delegate to underlying repository's shared cache
        return delegate.getUserFlow(id)
    }

    override suspend fun createUser(user: User): Boolean {
        val result = delegate.createUser(user)
        if (result) {
            // Invalidate caches on successful create
            invalidateAllCaches()
            Timber.d("CachingDataRepository: Invalidated caches after user creation")
        }
        return result
    }

    override suspend fun updateUser(user: User): Boolean {
        val result = delegate.updateUser(user)
        if (result) {
            // Optimistic update already handled by delegate
            // Just invalidate page and user list caches
            invalidatePageCaches()
            fullUserListCache = null
            Timber.d("CachingDataRepository: Invalidated caches after optimistic user update")
        }
        return result
    }

    override suspend fun deleteUser(id: Int): Boolean {
        val result = delegate.deleteUser(id)
        if (result) {
            // Optimistic delete already handled by delegate
            // Just invalidate all caches as list changed
            invalidateAllCaches()
            Timber.d("CachingDataRepository: Invalidated caches after optimistic user deletion")
        }
        return result
    }

    /**
     * Invalidates all page caches
     */
    private fun invalidatePageCaches() {
        pageCache.evictAll()
        totalCountCache = null
    }

    /**
     * Invalidates all caches
     */
    private fun invalidateAllCaches() {
        pageCache.evictAll()
        fullUserListCache = null
        totalCountCache = null
    }

    /**
     * Handles cache invalidation events from CacheEventBus
     */
    private fun handleCacheInvalidationEvent(event: CacheInvalidationEvent) {
        when (event) {
            is CacheInvalidationEvent.SyncCompleted -> {
                Timber.d("CachingDataRepository: Sync completed, invalidating all caches")
                invalidateAllCaches()
            }
            is CacheInvalidationEvent.UserCreated -> {
                Timber.d("CachingDataRepository: User ${event.userId} created, invalidating page and count caches")
                invalidatePageCaches()
                fullUserListCache = null
            }
            is CacheInvalidationEvent.UserUpdated -> {
                Timber.d("CachingDataRepository: User ${event.userId} updated, invalidating page caches")
                invalidatePageCaches()
                fullUserListCache = null
            }
            is CacheInvalidationEvent.UserDeleted -> {
                Timber.d("CachingDataRepository: User ${event.userId} deleted, invalidating all caches")
                invalidateAllCaches()
            }
            is CacheInvalidationEvent.InvalidateAll -> {
                Timber.d("CachingDataRepository: Manual invalidation requested")
                invalidateAllCaches()
            }
        }
    }

    /**
     * Gets cache statistics for monitoring
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            pageCacheSize = pageCache.size(),
            pageCacheMaxSize = pageCache.maxSize(),
            hasUserListCache = fullUserListCache?.isExpired() == false,
            hasCountCache = totalCountCache?.isExpired() == false
        )
    }

    data class CacheStats(
        val pageCacheSize: Int,
        val pageCacheMaxSize: Int,
        val hasUserListCache: Boolean,
        val hasCountCache: Boolean
    )

    /**
     * Implements Syncable interface to participate in sync operations
     * Delegates to the underlying repository and invalidates caches after sync
     */
    override suspend fun sync(): Boolean {
        Timber.d("CachingDataRepository: Starting sync")

        // If delegate implements Syncable, call its sync method
        val success = if (delegate is Syncable) {
            delegate.sync()
        } else {
            Timber.w("CachingDataRepository: Delegate does not implement Syncable")
            false
        }

        // Invalidate all caches after sync completes (regardless of success)
        // This ensures cache coherency even if sync partially succeeded
        if (success) {
            Timber.d("CachingDataRepository: Sync succeeded, invalidating all caches")
            invalidateAllCaches()
        } else {
            Timber.w("CachingDataRepository: Sync failed, but invalidating caches anyway for safety")
            invalidateAllCaches()
        }

        return success
    }
}

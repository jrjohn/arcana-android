package com.example.arcana.domain.repository.impl

import com.example.arcana.domain.model.User
import com.example.arcana.domain.repository.DataRepository
import com.example.arcana.sync.Syncable
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for CachingDataRepository
 * Note: Uses a real CacheEventBus (not mocked) to avoid SharedFlow issues
 */
class CachingDataRepositoryTest {

    private lateinit var delegate: DataRepository
    private lateinit var cacheEventBus: CacheEventBus
    private lateinit var repository: CachingDataRepository

    private val testUser1 = User(
        id = 1, firstName = "John", lastName = "Doe",
        email = "john@example.com", avatar = "avatar1.jpg"
    )
    private val testUser2 = User(
        id = 2, firstName = "Jane", lastName = "Smith",
        email = "jane@example.com", avatar = "avatar2.jpg"
    )
    private val testUsers = listOf(testUser1, testUser2)

    @Before
    fun setup() {
        delegate = mock()
        cacheEventBus = CacheEventBus()
        repository = CachingDataRepository(delegate, cacheEventBus)
    }

    // ========== getUsersPage Tests ==========

    @Test
    fun `getUsersPage fetches from delegate on cache miss`() = runTest {
        val page = 1
        val expected = Pair(testUsers, 5)
        whenever(delegate.getUsersPage(page)).thenReturn(Result.success(expected))

        val result = repository.getUsersPage(page)

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
        verify(delegate).getUsersPage(page)
    }

    @Test
    fun `getUsersPage returns delegate failure`() = runTest {
        val page = 1
        val exception = RuntimeException("Network error")
        whenever(delegate.getUsersPage(page)).thenReturn(Result.failure(exception))

        val result = repository.getUsersPage(page)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getUsersPage with different pages fetches each from delegate`() = runTest {
        val page1Data = Pair(listOf(testUser1), 2)
        val page2Data = Pair(listOf(testUser2), 2)
        whenever(delegate.getUsersPage(1)).thenReturn(Result.success(page1Data))
        whenever(delegate.getUsersPage(2)).thenReturn(Result.success(page2Data))

        val result1 = repository.getUsersPage(1)
        val result2 = repository.getUsersPage(2)

        assertEquals(page1Data, result1.getOrNull())
        assertEquals(page2Data, result2.getOrNull())
        verify(delegate).getUsersPage(1)
        verify(delegate).getUsersPage(2)
    }

    // ========== getTotalUserCount Tests ==========

    @Test
    fun `getTotalUserCount fetches from delegate on cache miss`() = runTest {
        whenever(delegate.getTotalUserCount()).thenReturn(42)

        val count = repository.getTotalUserCount()

        assertEquals(42, count)
        verify(delegate).getTotalUserCount()
    }

    @Test
    fun `getTotalUserCount caches result and returns same value`() = runTest {
        whenever(delegate.getTotalUserCount()).thenReturn(10)

        // First call fetches from delegate
        val count1 = repository.getTotalUserCount()
        // Second call should return cached value
        val count2 = repository.getTotalUserCount()

        assertEquals(10, count1)
        assertEquals(10, count2)
    }

    // ========== getUsers Tests ==========

    @Test
    fun `getUsers returns flow from delegate`() = runTest {
        whenever(delegate.getUsers()).thenReturn(flowOf(testUsers))

        val results = repository.getUsers().toList()

        assertEquals(listOf(testUsers), results)
    }

    @Test
    fun `getUsers returns empty list when delegate returns empty`() = runTest {
        whenever(delegate.getUsers()).thenReturn(flowOf(emptyList()))

        val results = repository.getUsers().toList()

        assertEquals(listOf(emptyList()), results)
    }

    // ========== getUserById Tests ==========

    @Test
    fun `getUserById delegates to underlying repository`() = runTest {
        whenever(delegate.getUserById(1)).thenReturn(Result.success(testUser1))

        val result = repository.getUserById(1)

        assertTrue(result.isSuccess)
        assertEquals(testUser1, result.getOrNull())
        verify(delegate).getUserById(1)
    }

    @Test
    fun `getUserById returns failure from delegate`() = runTest {
        whenever(delegate.getUserById(99)).thenReturn(Result.failure(RuntimeException("Not found")))

        val result = repository.getUserById(99)

        assertTrue(result.isFailure)
    }

    // ========== getUserFlow Tests ==========

    @Test
    fun `getUserFlow delegates to underlying repository`() = runTest {
        whenever(delegate.getUserFlow(1)).thenReturn(flowOf(testUser1))

        val results = repository.getUserFlow(1).toList()

        assertEquals(listOf(testUser1), results)
    }

    @Test
    fun `getUserFlow returns null flow from delegate`() = runTest {
        whenever(delegate.getUserFlow(99)).thenReturn(flowOf(null))

        val results = repository.getUserFlow(99).toList()

        assertEquals(listOf(null), results)
    }

    // ========== createUser Tests ==========

    @Test
    fun `createUser delegates to underlying repository and returns true on success`() = runTest {
        whenever(delegate.createUser(testUser1)).thenReturn(true)

        val result = repository.createUser(testUser1)

        assertTrue(result)
        verify(delegate).createUser(testUser1)
    }

    @Test
    fun `createUser delegates to underlying repository and returns false on failure`() = runTest {
        whenever(delegate.createUser(testUser1)).thenReturn(false)

        val result = repository.createUser(testUser1)

        assertFalse(result)
    }

    @Test
    fun `createUser invalidates caches on success`() = runTest {
        whenever(delegate.createUser(testUser1)).thenReturn(true)
        // Populate caches first
        whenever(delegate.getTotalUserCount()).thenReturn(5)
        repository.getTotalUserCount() // Cache total count

        repository.createUser(testUser1)

        // After create, the count should be fetched again (cache invalidated)
        whenever(delegate.getTotalUserCount()).thenReturn(6)
        repository.getTotalUserCount()
        verify(delegate, org.mockito.kotlin.times(2)).getTotalUserCount()
    }

    @Test
    fun `createUser does not invalidate caches on failure`() = runTest {
        whenever(delegate.createUser(testUser1)).thenReturn(false)
        whenever(delegate.getTotalUserCount()).thenReturn(5)
        repository.getTotalUserCount() // Cache total count

        repository.createUser(testUser1)

        // After failed create, the count should still be cached
        repository.getTotalUserCount()
        verify(delegate, org.mockito.kotlin.times(1)).getTotalUserCount()
    }

    // ========== updateUser Tests ==========

    @Test
    fun `updateUser delegates to underlying repository`() = runTest {
        whenever(delegate.updateUser(testUser1)).thenReturn(true)

        val result = repository.updateUser(testUser1)

        assertTrue(result)
        verify(delegate).updateUser(testUser1)
    }

    @Test
    fun `updateUser returns false when delegate fails`() = runTest {
        whenever(delegate.updateUser(testUser1)).thenReturn(false)

        val result = repository.updateUser(testUser1)

        assertFalse(result)
    }

    @Test
    fun `updateUser invalidates page caches on success`() = runTest {
        whenever(delegate.updateUser(testUser1)).thenReturn(true)
        whenever(delegate.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 1)))
        repository.getUsersPage(1) // Populate cache

        repository.updateUser(testUser1)

        // After update, page should be fetched again
        repository.getUsersPage(1)
        verify(delegate, org.mockito.kotlin.times(2)).getUsersPage(1)
    }

    // ========== deleteUser Tests ==========

    @Test
    fun `deleteUser delegates to underlying repository`() = runTest {
        whenever(delegate.deleteUser(1)).thenReturn(true)

        val result = repository.deleteUser(1)

        assertTrue(result)
        verify(delegate).deleteUser(1)
    }

    @Test
    fun `deleteUser returns false when delegate fails`() = runTest {
        whenever(delegate.deleteUser(1)).thenReturn(false)

        val result = repository.deleteUser(1)

        assertFalse(result)
    }

    // ========== invalidate Tests ==========

    @Test
    fun `invalidate clears all caches`() = runTest {
        // Populate caches
        whenever(delegate.getTotalUserCount()).thenReturn(5)
        repository.getTotalUserCount() // Cache total count

        repository.invalidate()

        // After invalidation, count should be fetched again
        repository.getTotalUserCount()
        verify(delegate, org.mockito.kotlin.times(2)).getTotalUserCount()
    }

    // ========== getCacheStats Tests ==========

    @Test
    fun `getCacheStats returns valid stats object`() {
        val stats = repository.getCacheStats()

        assertEquals(0, stats.pageCacheSize)
        assertFalse(stats.hasUserListCache)
        assertFalse(stats.hasCountCache)
    }

    @Test
    fun `getCacheStats pageCacheMaxSize is 20`() {
        val stats = repository.getCacheStats()
        assertEquals(20, stats.pageCacheMaxSize)
    }

    @Test
    fun `CacheStats data class equality works`() {
        val stats1 = CachingDataRepository.CacheStats(0, 20, false, false)
        val stats2 = CachingDataRepository.CacheStats(0, 20, false, false)
        assertEquals(stats1, stats2)
    }

    @Test
    fun `CacheStats toString works`() {
        val stats = CachingDataRepository.CacheStats(5, 20, true, false)
        val str = stats.toString()
        assertTrue(str.contains("5"))
        assertTrue(str.contains("20"))
    }

    // ========== sync Tests ==========

    @Test
    fun `sync delegates to Syncable delegate and returns true on success`() = runTest {
        val syncableDelegate = mock<DataAndSyncable>()
        whenever(syncableDelegate.getUsers()).thenReturn(flowOf(emptyList()))
        whenever(syncableDelegate.getUserFlow(any())).thenReturn(flowOf(null))
        whenever(syncableDelegate.sync()).thenReturn(true)

        val repo = CachingDataRepository(syncableDelegate, cacheEventBus)
        val result = repo.sync()

        assertTrue(result)
        verify(syncableDelegate).sync()
    }

    @Test
    fun `sync returns false for non-Syncable delegate`() = runTest {
        // delegate is a mock DataRepository (not Syncable)
        val result = repository.sync()

        assertFalse(result)
    }

    @Test
    fun `sync invalidates caches after completion`() = runTest {
        whenever(delegate.getTotalUserCount()).thenReturn(5)
        repository.getTotalUserCount() // Cache total count

        repository.sync()

        // After sync, caches should be invalidated
        repository.getTotalUserCount()
        verify(delegate, org.mockito.kotlin.times(2)).getTotalUserCount()
    }

    // ========== CacheInvalidationEvent Tests ==========

    @Test
    fun `handleCacheInvalidationEvent UserCreated invalidates page caches`() = runTest {
        whenever(delegate.getTotalUserCount()).thenReturn(5)
        repository.getTotalUserCount() // Populate cache

        cacheEventBus.emit(CacheInvalidationEvent.UserCreated(userId = 1))

        // Give coroutine a chance to process
        Thread.sleep(300)

        // Cache should be invalidated
        repository.getTotalUserCount()
        verify(delegate, org.mockito.kotlin.times(2)).getTotalUserCount()
    }

    @Test
    fun `handleCacheInvalidationEvent InvalidateAll clears all caches`() = runTest {
        whenever(delegate.getTotalUserCount()).thenReturn(5)
        repository.getTotalUserCount() // Populate cache

        cacheEventBus.emit(CacheInvalidationEvent.InvalidateAll)

        Thread.sleep(300)

        repository.getTotalUserCount()
        verify(delegate, org.mockito.kotlin.times(2)).getTotalUserCount()
    }

    @Test
    fun `handleCacheInvalidationEvent SyncCompleted invalidates all caches`() = runTest {
        whenever(delegate.getTotalUserCount()).thenReturn(5)
        repository.getTotalUserCount()

        cacheEventBus.emit(CacheInvalidationEvent.SyncCompleted)

        Thread.sleep(300)

        repository.getTotalUserCount()
        verify(delegate, org.mockito.kotlin.times(2)).getTotalUserCount()
    }

    @Test
    fun `handleCacheInvalidationEvent UserUpdated invalidates page caches`() = runTest {
        whenever(delegate.getTotalUserCount()).thenReturn(5)
        repository.getTotalUserCount()

        cacheEventBus.emit(CacheInvalidationEvent.UserUpdated(userId = 1))

        Thread.sleep(300)

        repository.getTotalUserCount()
        verify(delegate, org.mockito.kotlin.times(2)).getTotalUserCount()
    }

    @Test
    fun `handleCacheInvalidationEvent UserDeleted invalidates all caches`() = runTest {
        whenever(delegate.getTotalUserCount()).thenReturn(5)
        repository.getTotalUserCount()

        cacheEventBus.emit(CacheInvalidationEvent.UserDeleted(userId = 1))

        Thread.sleep(300)

        repository.getTotalUserCount()
        verify(delegate, org.mockito.kotlin.times(2)).getTotalUserCount()
    }
}

/**
 * Interface combining DataRepository and Syncable for testing sync functionality
 */
interface DataAndSyncable : DataRepository, Syncable

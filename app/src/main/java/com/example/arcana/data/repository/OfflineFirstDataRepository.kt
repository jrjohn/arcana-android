package com.example.arcana.data.repository

import com.example.arcana.core.common.NetworkMonitor
import com.example.arcana.domain.repository.DataRepository
import com.example.arcana.data.local.UserChangeDao
import com.example.arcana.data.local.UserDao
import com.example.arcana.data.model.ChangeType
import com.example.arcana.data.model.User
import com.example.arcana.data.model.UserChange
import com.example.arcana.data.network.UserNetworkDataSource
import com.example.arcana.data.remote.CreateUserRequest
import com.example.arcana.sync.Syncable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstDataRepository @Inject constructor(
    private val userDao: UserDao,
    private val userChangeDao: UserChangeDao,
    private val networkDataSource: UserNetworkDataSource,
    private val networkMonitor: NetworkMonitor,
    private val cacheEventBus: CacheEventBus
) : DataRepository, Syncable {

    // ============================================
    // Shared StateFlow Cache (Optimization #1)
    // ============================================
    /**
     * Shared in-memory cache of users
     * - Single source of truth for all ViewModels
     * - Automatically updated from Room Flow
     * - Enables instant access without DB queries
     */
    private val usersCache = MutableStateFlow<Map<Int, User>>(emptyMap())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Keep cache in sync with database
        // This ensures cache is always up-to-date
        scope.launch {
            userDao.getUsers().collect { users ->
                usersCache.update { users.associateBy { it.id } }
                Timber.d("UsersCache updated: ${users.size} users")
            }
        }
    }

    override fun getUsers(): Flow<List<User>> {
        // Return Room Flow (reactive to DB changes)
        return userDao.getUsers()
    }

    /**
     * Get user by ID from shared cache (instant, no DB query)
     * Falls back to DB if cache miss
     */
    override suspend fun getUserById(id: Int): Result<User> {
        return try {
            // Try cache first (Optimization #1)
            val cachedUser = usersCache.value[id]
            if (cachedUser != null) {
                Timber.d("Cache HIT for user $id")
                return Result.success(cachedUser)
            }

            // Cache miss - query DB
            Timber.d("Cache MISS for user $id - querying DB")
            val user = userDao.getUserById(id)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User with id $id not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user by id $id")
            Result.failure(e)
        }
    }

    /**
     * Get reactive Flow for specific user (Optimization #1)
     * Automatically emits when user changes
     */
    override fun getUserFlow(id: Int): Flow<User?> {
        return usersCache.map { cache -> cache[id] }
    }

    override suspend fun getUsersPage(page: Int): Result<Pair<List<User>, Int>> {
        return try {
            if (!networkMonitor.isOnline.first()) {
                // When offline, read from local database
                Timber.d("Offline mode: Reading page $page from local database")
                val allLocalUsers = userDao.getUsers().first()

                // Calculate pagination from local data
                val pageSize = 6 // Match API page size
                val totalPages = (allLocalUsers.size + pageSize - 1) / pageSize // Ceiling division

                if (page < 1 || (allLocalUsers.isNotEmpty() && page > totalPages)) {
                    Timber.w("Invalid page $page requested (total pages: $totalPages)")
                    return Result.failure(Exception("Invalid page number"))
                }

                // Get users for requested page
                val startIndex = (page - 1) * pageSize
                val endIndex = minOf(startIndex + pageSize, allLocalUsers.size)
                val pageUsers = if (startIndex < allLocalUsers.size) {
                    allLocalUsers.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }

                Timber.d("Offline mode: Returning ${pageUsers.size} users for page $page/$totalPages")
                Result.success(Pair(pageUsers, maxOf(totalPages, 1)))
            } else {
                // When online, fetch from network
                Timber.d("Online mode: Fetching page $page from network")
                val (users, totalPages) = networkDataSource.getUsersPage(page)
                Result.success(Pair(users, totalPages))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get users page $page")
            Result.failure(e)
        }
    }

    override suspend fun sync(): Boolean {
        if (!networkMonitor.isOnline.first()) {
            Timber.d("Sync skipped: Device is offline")
            return false
        }

        try {
            Timber.d("Starting sync process")
            processOfflineChanges()

            // Fetch all pages of users from the network
            Timber.d("Fetching all users from network")
            val allNetworkUsers = mutableListOf<User>()
            var currentPage = 1
            var totalPages = 1

            do {
                val (pageUsers, pages) = networkDataSource.getUsersPage(currentPage)
                totalPages = pages
                allNetworkUsers.addAll(pageUsers)
                Timber.d("Fetched page $currentPage/$totalPages with ${pageUsers.size} users")
                currentPage++
            } while (currentPage <= totalPages)

            Timber.d("Received ${allNetworkUsers.size} total users from network across $totalPages pages")

            // Get local users for conflict resolution
            val localUsers = userDao.getUsers().first()
            Timber.d("Found ${localUsers.size} local users for conflict resolution")

            // Resolve conflicts and merge data
            val resolvedUsers = resolveConflicts(localUsers, allNetworkUsers)
            Timber.d("Resolved conflicts, inserting ${resolvedUsers.size} users")

            userDao.insertUsers(resolvedUsers)
            Timber.d("Sync completed successfully")

            // Emit cache invalidation event after successful sync
            cacheEventBus.emit(CacheInvalidationEvent.SyncCompleted)

            return true
        } catch (e: Exception) {
            Timber.e(e, "Sync failed for DataRepository")
            return false
        }
    }

    /**
     * Resolves conflicts between local and network user data
     * Strategy: Last-write-wins based on updatedAt timestamp
     *
     * @param localUsers Users from local database
     * @param networkUsers Users from network
     * @return Merged list of users with conflicts resolved
     */
    private suspend fun resolveConflicts(
        localUsers: List<User>,
        networkUsers: List<User>
    ): List<User> {
        val localMap = localUsers.associateBy { it.id }
        val networkMap = networkUsers.associateBy { it.id }
        val resolvedUsers = mutableListOf<User>()
        var conflictsDetected = 0
        var conflictsResolved = 0

        // Process all network users
        networkMap.forEach { (id, networkUser) ->
            val localUser = localMap[id]

            if (localUser == null) {
                // User only exists on network, add it
                Timber.d("ConflictResolution: User $id only on network, adding")
                resolvedUsers.add(networkUser)
            } else {
                // User exists both locally and on network, resolve conflict
                val resolved = resolveUserConflict(localUser, networkUser)
                if (resolved != networkUser) {
                    conflictsDetected++
                    if (resolved == localUser) {
                        Timber.d("ConflictResolution: User $id - local version newer, keeping local")
                        // Local version is newer, push to network
                        try {
                            networkDataSource.updateUser(
                                localUser.id,
                                CreateUserRequest(localUser.name, "Developer")
                            )
                            conflictsResolved++
                            Timber.d("ConflictResolution: Successfully pushed local user $id to network")
                        } catch (e: Exception) {
                            Timber.e(e, "ConflictResolution: Failed to push local user $id to network")
                        }
                    } else {
                        Timber.d("ConflictResolution: User $id - network version newer, using network")
                    }
                }
                resolvedUsers.add(resolved)
            }
        }

        // Add any users that only exist locally (not on network)
        localMap.forEach { (id, localUser) ->
            if (!networkMap.containsKey(id)) {
                Timber.d("ConflictResolution: User $id only exists locally, keeping")
                resolvedUsers.add(localUser)
            }
        }

        if (conflictsDetected > 0) {
            Timber.d("ConflictResolution: Detected $conflictsDetected conflicts, resolved $conflictsResolved")
        }

        return resolvedUsers
    }

    /**
     * Resolves conflict for a single user using last-write-wins strategy
     * Compares timestamps and versions to determine which version to keep
     *
     * @param localUser User from local database
     * @param networkUser User from network
     * @return The user version to keep
     */
    private fun resolveUserConflict(localUser: User, networkUser: User): User {
        // Compare timestamps first
        return when {
            localUser.updatedAt > networkUser.updatedAt -> {
                // Local is newer
                Timber.v("ConflictResolution: User ${localUser.id} - local timestamp ${localUser.updatedAt} > network ${networkUser.updatedAt}")
                localUser
            }
            localUser.updatedAt < networkUser.updatedAt -> {
                // Network is newer
                Timber.v("ConflictResolution: User ${localUser.id} - network timestamp ${networkUser.updatedAt} > local ${localUser.updatedAt}")
                networkUser
            }
            else -> {
                // Same timestamp, compare versions
                if (localUser.version > networkUser.version) {
                    Timber.v("ConflictResolution: User ${localUser.id} - same timestamp, local version ${localUser.version} > network ${networkUser.version}")
                    localUser
                } else {
                    Timber.v("ConflictResolution: User ${localUser.id} - same timestamp, network version ${networkUser.version} >= local ${localUser.version}")
                    networkUser
                }
            }
        }
    }

    override suspend fun getTotalUserCount(): Int {
        return try {
            if (networkMonitor.isOnline.first()) {
                val (_, total) = networkDataSource.getUsersWithTotal()
                total
            } else {
                userDao.getUsers().first().size
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get total user count")
            userDao.getUsers().first().size
        }
    }

    override suspend fun createUser(user: User): Boolean {
        if (networkMonitor.isOnline.first()) {
            return try {
                networkDataSource.createUser(CreateUserRequest(user.name, "Developer"))
                sync()
                // Emit cache invalidation event (sync already emits SyncCompleted, but emit UserCreated for clarity)
                cacheEventBus.emit(CacheInvalidationEvent.InvalidateAll)
                true
            } catch (e: Exception) {
                Timber.e(e, "Failed to create user online, will queue for offline")
                queueCreateUser(user)
                false
            }
        } else {
            queueCreateUser(user)
            // Emit event even for offline creation
            cacheEventBus.emit(CacheInvalidationEvent.InvalidateAll)
            return true
        }
    }

    /**
     * Optimistic Update (Optimization #3)
     * Updates DB immediately for instant UI feedback
     * Then syncs with network in background
     */
    override suspend fun updateUser(user: User): Boolean {
        // Step 1: Update local DB immediately (instant UI update)
        queueUpdateUser(user)
        cacheEventBus.emit(CacheInvalidationEvent.UserUpdated(user.id))
        Timber.d("Optimistic update: Updated user ${user.id} locally")

        // Step 2: Sync with network in background
        if (networkMonitor.isOnline.first()) {
            try {
                networkDataSource.updateUser(user.id, CreateUserRequest(user.name, "Developer"))
                Timber.d("Optimistic update: Network sync successful for user ${user.id}")
                // Sync again to get any server-side changes
                sync()
            } catch (e: Exception) {
                Timber.w(e, "Optimistic update: Network sync failed for user ${user.id}, will retry later")
                // Change is already queued, will sync later
            }
        }
        return true
    }

    /**
     * Optimistic Delete (Optimization #3)
     * Deletes from DB immediately for instant UI feedback
     * Then syncs with network in background
     */
    override suspend fun deleteUser(id: Int): Boolean {
        // Step 1: Delete from local DB immediately (instant UI update)
        queueDeleteUser(id)
        cacheEventBus.emit(CacheInvalidationEvent.UserDeleted(id))
        Timber.d("Optimistic delete: Deleted user $id locally")

        // Step 2: Sync with network in background
        if (networkMonitor.isOnline.first()) {
            try {
                networkDataSource.deleteUser(id)
                Timber.d("Optimistic delete: Network sync successful for user $id")
                sync()
            } catch (e: Exception) {
                Timber.w(e, "Optimistic delete: Network sync failed for user $id, will retry later")
                // Change is already queued, will sync later
            }
        }
        return true
    }

    private suspend fun queueCreateUser(user: User) {
        val tempId = UUID.randomUUID().hashCode()
        userDao.upsertUser(
            user.copy(id = tempId)
        )
        userChangeDao.insert(
            UserChange(
                userId = tempId,
                type = ChangeType.CREATE,
                name = user.name,
                job = "Developer"
            )
        )
    }

    private suspend fun queueUpdateUser(user: User) {
        userDao.upsertUser(user)
        userChangeDao.insert(
            UserChange(
                userId = user.id,
                type = ChangeType.UPDATE,
                name = user.name,
                job = "Developer"
            )
        )
    }

    private suspend fun queueDeleteUser(id: Int) {
        userDao.deleteUser(User(id = id))
        userChangeDao.insert(UserChange(userId = id, type = ChangeType.DELETE))
    }

    private suspend fun processOfflineChanges() {
        val pendingChanges = userChangeDao.getAll()
        Timber.d("Processing ${pendingChanges.size} offline changes")
        val processedIds = mutableListOf<Long>()

        pendingChanges.forEach { change ->
            try {
                Timber.d("Processing offline change: ${change.type} for user ${change.userId}")
                when (change.type) {
                    ChangeType.CREATE -> {
                        networkDataSource.createUser(
                            CreateUserRequest(
                                requireNotNull(change.name) { "name is required for CREATE change" },
                                requireNotNull(change.job) { "job is required for CREATE change" }
                            )
                        )
                        Timber.d("Successfully processed CREATE for user ${change.userId}")
                    }
                    ChangeType.UPDATE -> {
                        networkDataSource.updateUser(
                            change.userId,
                            CreateUserRequest(
                                requireNotNull(change.name) { "name is required for UPDATE change" },
                                requireNotNull(change.job) { "job is required for UPDATE change" }
                            )
                        )
                        Timber.d("Successfully processed UPDATE for user ${change.userId}")
                    }
                    ChangeType.DELETE -> {
                        networkDataSource.deleteUser(change.userId)
                        Timber.d("Successfully processed DELETE for user ${change.userId}")
                    }
                }
                processedIds.add(change.id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to process offline change: $change")
            }
        }
        if (processedIds.isNotEmpty()) {
            userChangeDao.delete(processedIds)
            Timber.d("Deleted ${processedIds.size} processed offline changes from queue")
        }
    }
}

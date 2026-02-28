package com.example.arcana.domain.service.impl

import com.example.arcana.data.model.User
import com.example.arcana.domain.repository.impl.CacheEventBus
import com.example.arcana.domain.repository.impl.CacheInvalidationEvent
import com.example.arcana.domain.repository.DataRepository
import com.example.arcana.domain.service.UserService
import com.example.arcana.sync.Synchronizer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserServiceImpl @Inject constructor(
    private val dataRepository: DataRepository,
    private val synchronizer: Synchronizer,
    private val cacheEventBus: CacheEventBus
) : UserService {

    override fun getUsers(): Flow<List<User>> {
        return dataRepository.getUsers()
    }

    override fun getUserFlow(id: Int): Flow<User?> {
        return dataRepository.getUserFlow(id)
    }

    override suspend fun getUserById(id: Int): Result<User> {
        return dataRepository.getUserById(id)
    }

    override suspend fun getUsersPage(page: Int): Result<Pair<List<User>, Int>> {
        return dataRepository.getUsersPage(page)
    }

    override suspend fun getTotalUserCount(): Int {
        return dataRepository.getTotalUserCount()
    }

    override suspend fun createUser(user: User): Boolean {
        return dataRepository.createUser(user)
    }

    override suspend fun updateUser(user: User): Boolean {
        return dataRepository.updateUser(user)
    }

    override suspend fun deleteUser(id: Int): Boolean {
        return dataRepository.deleteUser(id)
    }

    override suspend fun syncUsers(): Boolean {
        return synchronizer.sync()
    }

    override fun invalidateCache() {
        cacheEventBus.tryEmit(CacheInvalidationEvent.InvalidateAll)
    }
}

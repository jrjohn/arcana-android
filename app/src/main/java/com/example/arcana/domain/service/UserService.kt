package com.example.arcana.domain.service

import com.example.arcana.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserService {
    fun getUsers(): Flow<List<User>>
    fun getUserFlow(id: Int): Flow<User?>
    suspend fun getUserById(id: Int): Result<User>
    suspend fun getUsersPage(page: Int): Result<Pair<List<User>, Int>>
    suspend fun getTotalUserCount(): Int
    suspend fun createUser(user: User): Boolean
    suspend fun updateUser(user: User): Boolean
    suspend fun deleteUser(id: Int): Boolean
    suspend fun syncUsers(): Boolean
    fun invalidateCache()
}
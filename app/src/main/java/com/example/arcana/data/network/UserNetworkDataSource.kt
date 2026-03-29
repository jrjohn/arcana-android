package com.example.arcana.data.network

import com.example.arcana.domain.model.User
import com.example.arcana.data.remote.ApiService
import com.example.arcana.data.remote.CreateUserRequest
import com.example.arcana.data.remote.UserDto
import timber.log.Timber
import javax.inject.Inject

class UserNetworkDataSource @Inject constructor(
    private val apiService: ApiService,
) {
    suspend fun getUsers(): List<User> {
        Timber.d("API Request: GET /users")
        val response = apiService.getUsers()
        Timber.d("API Response: GET /users - ${response.data.size} users received")
        return response.data.map { it.toUser() }
    }

    suspend fun getUsersWithTotal(): Pair<List<User>, Int> {
        Timber.d("API Request: GET /users (with total)")
        val response = apiService.getUsers()
        Timber.d("API Response: GET /users - ${response.data.size} users, total: ${response.total}")
        return Pair(response.data.map { it.toUser() }, response.total)
    }

    suspend fun getUsersPage(page: Int): Pair<List<User>, Int> {
        Timber.d("API Request: GET /users?page=$page")
        val response = apiService.getUsersPage(page)
        Timber.d("API Response: GET /users?page=$page - ${response.data.size} users, totalPages: ${response.totalPages}")
        return Pair(response.data.map { it.toUser() }, response.totalPages)
    }

    suspend fun createUser(request: CreateUserRequest) {
        Timber.d("API Request: POST /users - name=${request.name}, job=${request.job}")
        val response = apiService.createUser(request)
        Timber.d("API Response: POST /users - id=${response.id}, name=${response.name}, createdAt=${response.createdAt}")
    }

    suspend fun updateUser(id: Int, request: CreateUserRequest) {
        Timber.d("API Request: PUT /users/$id - name=${request.name}, job=${request.job}")
        val response = apiService.updateUser(id, request)
        Timber.d("API Response: PUT /users/$id - name=${response.name}, job=${response.job}, updatedAt=${response.updatedAt}")
    }

    suspend fun deleteUser(id: Int) {
        Timber.d("API Request: DELETE /users/$id")
        apiService.deleteUser(id)
        Timber.d("API Response: DELETE /users/$id - Success")
    }
}

private fun UserDto.toUser(): User {
    return User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        avatar = avatar
    )
}

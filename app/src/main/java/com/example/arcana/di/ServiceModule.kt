package com.example.arcana.di

import com.example.arcana.domain.service.UserService
import com.example.arcana.domain.service.impl.UserServiceImpl
import dagger.Binds
import dagger.Module // NOSONAR kotlin:S1128
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule { // NOSONAR kotlin:S6526

    @Binds
    @Singleton
    abstract fun bindUserService(
        userServiceImpl: UserServiceImpl
    ): UserService
}
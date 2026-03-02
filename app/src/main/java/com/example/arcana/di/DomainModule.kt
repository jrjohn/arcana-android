package com.example.arcana.di

import com.example.arcana.core.common.AndroidStringProvider
import com.example.arcana.core.common.RetryPolicy
import com.example.arcana.core.common.StringProvider
import com.example.arcana.domain.validation.UserValidator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    @Singleton
    abstract fun bindStringProvider(
        androidStringProvider: AndroidStringProvider
    ): StringProvider

    companion object {
        @Provides
        @Singleton
        fun provideUserValidator(stringProvider: StringProvider): UserValidator {
            return UserValidator(stringProvider)
        }

        @Provides
        @Singleton
        fun provideRetryPolicy(): RetryPolicy {
            return RetryPolicy.forNetworkOperations()
        }
    }
}

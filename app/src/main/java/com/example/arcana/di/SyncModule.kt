package com.example.arcana.di

import com.example.arcana.sync.SyncManager
import com.example.arcana.sync.Synchronizer
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule { // NOSONAR kotlin:S6526

    @Binds
    @Singleton
    abstract fun bindSynchronizer(
        syncManager: SyncManager
    ): Synchronizer
}

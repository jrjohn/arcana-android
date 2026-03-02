package com.example.arcana.di

import com.example.arcana.domain.repository.impl.CacheEventBus
import com.example.arcana.domain.repository.impl.CachingDataRepository
import com.example.arcana.domain.repository.DataRepository
import com.example.arcana.domain.repository.impl.OfflineFirstDataRepository
import com.example.arcana.sync.Syncable
import dagger.Binds
import dagger.Module // NOSONAR kotlin:S1128
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OfflineFirst

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Cached

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    companion object {
        /**
         * Provides the base OfflineFirstDataRepository (without caching)
         */
        @Provides
        @Singleton
        @OfflineFirst
        fun provideOfflineFirstDataRepository(
            offlineFirstDataRepository: OfflineFirstDataRepository
        ): DataRepository = offlineFirstDataRepository

        /**
         * Provides the cached version of the repository (wraps OfflineFirstDataRepository)
         */
        @Provides
        @Singleton
        @Cached
        fun provideCachedDataRepository(
            @OfflineFirst offlineFirstDataRepository: DataRepository,
            cacheEventBus: CacheEventBus
        ): DataRepository = CachingDataRepository(offlineFirstDataRepository, cacheEventBus)
    }

    /**
     * Binds the cached repository as the default DataRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindDataRepository(
        @Cached cachingDataRepository: DataRepository
    ): DataRepository

    /**
     * Binds CachingDataRepository (the top-level wrapper) into the set of Syncable components
     * This ensures cache invalidation happens synchronously during sync operations
     * CachingDataRepository delegates to OfflineFirstDataRepository and invalidates caches after
     */
    @Binds
    @IntoSet
    abstract fun bindCachingDataRepositoryAsSyncable(
        cachingDataRepository: CachingDataRepository
    ): Syncable
}

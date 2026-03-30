package com.example.arcana.di

import com.example.arcana.core.analytics.AnalyticsTracker
import com.example.arcana.core.analytics.AnalyticsUploadScheduler
import com.example.arcana.data.analytics.PersistentAnalyticsTracker
import com.example.arcana.data.network.AnalyticsApiService
import com.example.arcana.data.network.createAnalyticsApiService
import com.example.arcana.data.worker.AnalyticsUploadSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.jensklingenberg.ktorfit.Ktorfit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(
        persistentAnalyticsTracker: PersistentAnalyticsTracker
    ): AnalyticsTracker

    @Binds
    @Singleton
    abstract fun bindAnalyticsUploadScheduler(
        impl: AnalyticsUploadSchedulerImpl
    ): AnalyticsUploadScheduler

    companion object {
        @Provides
        @Singleton
        fun provideAnalyticsApiService(ktorfit: Ktorfit): AnalyticsApiService {
            return ktorfit.createAnalyticsApiService()
        }
    }
}

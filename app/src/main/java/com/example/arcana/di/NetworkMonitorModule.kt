package com.example.arcana.di

import com.example.arcana.core.common.ConnectivityManagerNetworkMonitor
import com.example.arcana.core.common.NetworkMonitor
import dagger.Binds
import dagger.Module // NOSONAR kotlin:S1128
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorModule { // NOSONAR kotlin:S6526

    @Binds
    abstract fun bindNetworkMonitor(
        networkMonitor: ConnectivityManagerNetworkMonitor
    ): NetworkMonitor
}

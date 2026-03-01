package com.example.arcana.sync

interface Synchronizer { // NOSONAR kotlin:S6517
    suspend fun sync(): Boolean
}

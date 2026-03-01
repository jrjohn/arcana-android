package com.example.arcana.sync

interface Syncable { // NOSONAR kotlin:S6517
    suspend fun sync(): Boolean
}

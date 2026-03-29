package com.example.arcana.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.arcana.data.local.dao.AnalyticsEventDao
import com.example.arcana.data.local.entity.AnalyticsEventEntity
import com.example.arcana.domain.model.User
import com.example.arcana.domain.model.UserChange

@Database(
    entities = [User::class, UserChange::class, AnalyticsEventEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userChangeDao(): UserChangeDao
    abstract fun analyticsEventDao(): AnalyticsEventDao
}

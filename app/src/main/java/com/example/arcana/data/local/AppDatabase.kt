package com.example.arcana.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.arcana.data.local.dao.AnalyticsEventDao
import com.example.arcana.data.local.entity.AnalyticsEventEntity
import com.example.arcana.domain.model.User
import com.example.arcana.domain.model.UserChange

@Database(
    entities = [
        User::class,
        UserChange::class,
        AnalyticsEventEntity::class,
        // new-feature:entities (tools/new-feature.sh inserts above this line)
    ],
    version = 5, // new-feature:version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userChangeDao(): UserChangeDao
    abstract fun analyticsEventDao(): AnalyticsEventDao
    // new-feature:daos (tools/new-feature.sh inserts above this line)
}

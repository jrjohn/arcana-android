package com.example.arcana.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.arcana.data.local.AppDatabase
import com.example.arcana.data.local.UserChangeDao
import com.example.arcana.data.local.UserDao
import com.example.arcana.data.local.dao.AnalyticsEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add updatedAt and version columns to users table
            db.execSQL("ALTER TABLE User ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            db.execSQL("ALTER TABLE User ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create analytics_events table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS analytics_events (
                    eventId TEXT PRIMARY KEY NOT NULL,
                    eventType TEXT NOT NULL,
                    eventName TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    sessionId TEXT NOT NULL,
                    userId TEXT,
                    screenName TEXT,
                    params TEXT NOT NULL,
                    deviceInfo TEXT NOT NULL,
                    appInfo TEXT NOT NULL,
                    uploaded INTEGER NOT NULL DEFAULT 0,
                    uploadAttempts INTEGER NOT NULL DEFAULT 0,
                    lastUploadAttempt INTEGER,
                    createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}
                )
            """.trimIndent())
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "arcana.db"
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration(dropAllTables = true) // Fallback only if migration fails
            .build()
    }

    @Provides
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    fun provideUserChangeDao(appDatabase: AppDatabase): UserChangeDao {
        return appDatabase.userChangeDao()
    }

    @Provides
    fun provideAnalyticsEventDao(appDatabase: AppDatabase): AnalyticsEventDao {
        return appDatabase.analyticsEventDao()
    }
}

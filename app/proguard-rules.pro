# ============================================================
# Arcana Android - ProGuard / R8 Rules
# ============================================================

# Keep stack trace line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep the BuildConfig
-keep class com.arcana.example.BuildConfig { *; }

# ============================================================
# Kotlinx Serialization
# ============================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

-keep,includedescriptorclasses class com.example.arcana.**$$serializer { *; }
-keepclassmembers class com.example.arcana.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.arcana.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================
# Ktor
# ============================================================
-keep class io.ktor.** { *; }
-keep class io.ktor.client.** { *; }
-dontwarn io.ktor.**

# ============================================================
# Room Database
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ============================================================
# Hilt / Dagger (plugin auto-includes rules, but just in case)
# ============================================================
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ============================================================
# Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================================
# Jetpack Compose
# ============================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================
# WorkManager
# ============================================================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ============================================================
# Timber
# ============================================================
-dontwarn org.jetbrains.annotations.**

# ============================================================
# Kotlin Reflection
# ============================================================
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontnote kotlin.**

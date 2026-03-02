package com.example.arcana.di

import com.example.arcana.BuildConfig
import com.example.arcana.data.remote.ApiService
import com.example.arcana.data.remote.JsonLoggingInterceptor
import com.example.arcana.data.remote.createApiService
import de.jensklingenberg.ktorfit.Ktorfit
import dagger.Module // NOSONAR kotlin:S1128
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Singleton

private const val API_KEY_HEADER = "x-api-key"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }

            // Option 1: Built-in Ktor Logging (basic logging)
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.tag("HTTP Client").v(message)
                    }
                }
                level = LogLevel.HEADERS  // Use BODY for full logging, HEADERS for headers only
                sanitizeHeader { header -> header == API_KEY_HEADER }
            }

            // Option 2: Custom JSON Logging Interceptor (formatted JSON logging)
            // Uncomment to use custom interceptor instead of or in addition to built-in logging
            install(JsonLoggingInterceptor) {
                enabled = BuildConfig.DEBUG  // Only log in debug builds
                logHeaders = true
                sanitizedHeaders = setOf(API_KEY_HEADER, "Authorization", "Cookie")
            }

            defaultRequest {
                // Use API key from BuildConfig
                header(API_KEY_HEADER, BuildConfig.API_KEY)
                contentType(ContentType.Application.Json)
            }

            expectSuccess = true
        }
    }

    @Provides
    @Singleton
    fun provideKtorfit(httpClient: HttpClient): Ktorfit {
        return Ktorfit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .httpClient(httpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(ktorfit: Ktorfit): ApiService {
        return ktorfit.createApiService()
    }

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
}

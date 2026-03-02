package com.example.arcana.data.remote

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readRemaining
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Configuration for JSON logging interceptor
 */
class JsonLoggingConfig {
    var enabled: Boolean = true
    var logHeaders: Boolean = true
    var sanitizedHeaders: Set<String> = setOf("Authorization", "x-api-key", "Cookie")
}

/**
 * Custom Ktor plugin that logs JSON request and response bodies in a formatted way.
 *
 * This plugin provides structured logging for HTTP requests and responses with:
 * - Method, URL, and status code
 * - Request/response headers with sanitization
 * - Pretty-printed JSON bodies
 * - Request duration
 *
 * Usage in NetworkModule:
 * ```kotlin
 * HttpClient(CIO) {
 *     install(JsonLoggingInterceptor) {
 *         enabled = true
 *         logHeaders = true
 *         sanitizedHeaders = setOf("Authorization", "x-api-key")
 *     }
 * }
 * ```
 */
private object LoggingConstants {
    const val TAG_REQUEST = "API Request"
    const val TAG_RESPONSE = "API Response"
    const val SEPARATOR = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

val JsonLoggingInterceptor = createClientPlugin("JsonLoggingInterceptor", ::JsonLoggingConfig) {
    val config = pluginConfig
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // Intercept and log requests
    onRequest { request, _ ->
        if (!config.enabled) return@onRequest

        val method = request.method.value
        val url = request.url.toString()

        Timber.tag(LoggingConstants.TAG_REQUEST).d(LoggingConstants.SEPARATOR)
        Timber.tag(LoggingConstants.TAG_REQUEST).d("→ $method $url")

        if (config.logHeaders) {
            val headers = request.headers.entries()
                .joinToString("\n") { (key, values) ->
                    val value = if (config.sanitizedHeaders.contains(key)) {
                        "***REDACTED***"
                    } else {
                        values.joinToString()
                    }
                    "  $key: $value"
                }
            if (headers.isNotEmpty()) {
                Timber.tag(LoggingConstants.TAG_REQUEST).d("Headers:\n$headers")
            }
        }
    }

    // Transform response to log the body without consuming it
    transformResponseBody { response, content, _ ->
        if (!config.enabled) return@transformResponseBody content

        val contentType = response.contentType()
        if (contentType?.match(ContentType.Application.Json) == true) {
            try {
                // Read the entire body
                val channel = content as? ByteReadChannel ?: return@transformResponseBody content
                val packet = channel.readRemaining()
                @Suppress("DEPRECATION")
                val bytes = packet.readBytes()
                val bodyText = bytes.toString(Charsets.UTF_8)

                // Log the response
                logResponse(response, bodyText, json, config)

                // Return a new channel with the same data so it can be consumed by the caller
                ByteReadChannel(bytes)
            } catch (e: Exception) {
                Timber.tag(LoggingConstants.TAG_RESPONSE).e(e, "Failed to log response body")
                content
            }
        } else {
            logResponse(response, null, json, config)
            content
        }
    }
}

private fun logResponse(
    response: io.ktor.client.statement.HttpResponse,
    bodyText: String?,
    json: Json,
    config: JsonLoggingConfig
) {
    val method = response.call.request.method.value
    val url = response.call.request.url.toString()
    val status = response.status.value
    val statusDesc = response.status.description
    val duration = response.responseTime.timestamp - response.requestTime.timestamp

    Timber.tag(LoggingConstants.TAG_RESPONSE).d(LoggingConstants.SEPARATOR)
    Timber.tag(LoggingConstants.TAG_RESPONSE).d("← $method $url")
    Timber.tag(LoggingConstants.TAG_RESPONSE).d("Status: $status $statusDesc (${duration}ms)")

    if (config.logHeaders) {
        val headers = response.headers.entries()
            .joinToString("\n") { (key, values) ->
                "  $key: ${values.joinToString()}"
            }
        if (headers.isNotEmpty()) {
            Timber.tag(LoggingConstants.TAG_RESPONSE).d("Headers:\n$headers")
        }
    }

    // Log body if available
    if (bodyText != null) {
        if (bodyText.isNotEmpty()) {
            val prettyJson = try {
                val jsonElement = json.parseToJsonElement(bodyText)
                json.encodeToString(
                    kotlinx.serialization.json.JsonElement.serializer(),
                    jsonElement
                )
            } catch (e: Exception) {
                bodyText // Not valid JSON, use as-is
            }
            Timber.tag(LoggingConstants.TAG_RESPONSE).d("JSON Body:\n$prettyJson")
        } else {
            Timber.tag(LoggingConstants.TAG_RESPONSE).d("Body: [Empty]")
        }
    }
    Timber.tag(LoggingConstants.TAG_RESPONSE).d(LoggingConstants.SEPARATOR)
}

package com.example.arcana.data.network

import com.example.arcana.core.analytics.AnalyticsEvent
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
import kotlinx.serialization.Serializable

/**
 * API service for uploading analytics events to the cloud
 */
interface AnalyticsApiService { // NOSONAR kotlin:S6517

    /**
     * Upload a batch of analytics events
     *
     * @param request Batch upload request containing events
     * @return Upload response with status
     */
    @POST("analytics/events/batch")
    suspend fun uploadEvents(@Body request: BatchUploadRequest): BatchUploadResponse
}

/**
 * Request body for batch upload
 */
@Serializable
data class BatchUploadRequest(
    val events: List<AnalyticsEvent>,
    val deviceId: String,
    val uploadTimestamp: Long = System.currentTimeMillis()
)

/**
 * Response from batch upload
 */
@Serializable
data class BatchUploadResponse(
    val success: Boolean,
    val processedCount: Int,
    val failedCount: Int = 0,
    val message: String? = null,
    val failedEventIds: List<String> = emptyList()
)

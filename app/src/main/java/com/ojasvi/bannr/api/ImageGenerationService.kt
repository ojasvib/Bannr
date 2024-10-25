package com.ojasvi.bannr.api
import retrofit2.http.Body
import retrofit2.http.POST

data class ImageRequest(val prompt: String)

data class ImageResponse(val imageUrl: String)

interface ImageGenerationService {
    @POST("/") // Replace with your actual endpoint
    suspend fun generateImage(@Body request: ImageRequest): ImageResponse
}
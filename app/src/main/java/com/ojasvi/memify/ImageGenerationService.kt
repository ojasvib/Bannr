import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class ImageRequest(val prompt: String) // Adjust according to your API requirements

data class ImageResponse(val image_url: String) // Adjust according to your API response

interface ImageGenerationService {
    @POST("/") // Replace with your actual endpoint
    fun generateImage(@Body request: ImageRequest): Call<ImageResponse>
}
package com.ojasvi.bannr.ui.screens

import ImageRequest
import ImageResponse
import android.app.Application
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.SuccessResult
import coil.size.Precision
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.ojasvi.bannr.BuildConfig.API_KEY
import com.ojasvi.bannr.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PhotoReasoningViewModel(
    private val app: Application
) : AndroidViewModel(app) {

    private val _uiState: MutableStateFlow<PhotoReasoningUiState> =
        MutableStateFlow(PhotoReasoningUiState.Initial)
    val uiState: StateFlow<PhotoReasoningUiState> =
        _uiState.asStateFlow()

    private val config = generationConfig {
        temperature = 0.7f
    }
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = API_KEY,
        generationConfig = config
    )

    fun resetToInitialUiState() {
        _uiState.value = PhotoReasoningUiState.Initial
    }

    fun generatePromoBanner(userPrompt: String) {
//                            generateImage(uiState.outputText+" Use preceding information about number of products " +
//                                    "followed by the products to generate a visually appealing promotional banner." +
//                                    "Include the following customization details: "+prompt+"The banner should highlight the " +
//                                    "listed products, using bold text for the promotion and a suitable call-to-action like " +
//                                    "'Celebrate in Style!'. Include decorative elements related to the theme (such as 'diya " +
//                                    "lamps, sparkles, and floral patterns for Diwali') to enhance the overall design"
//                            ){imageUrl ->
        when (uiState.value) {
            is PhotoReasoningUiState.GeminiSuccess -> {
                val geminiInput =
                    (uiState.value as PhotoReasoningUiState.GeminiSuccess).geminiTextResponse

                var promppp =
                    "Create Promo Banner that has products $geminiInput and has an awesome catchy headline to attract customers"
                if (userPrompt.isNotBlank()) {
                    promppp += "Include the following customization details: $userPrompt"
                }
                generateImage(
                    promppp
                ) { imageUrl ->
                    if (imageUrl == null) {
                        _uiState.value = PhotoReasoningUiState.Error("imagenError")
                    }
                    else {
                        _uiState.value = PhotoReasoningUiState.ImagenSuccess(imageUrl)
                    }
                }
            }

            else -> {}
        }
    }

    fun reasonWithGemini(
        selectedImages: List<Uri>
    ) {
        _uiState.value = PhotoReasoningUiState.Loading
        val prompt =
            "Meticulously tell what products are present in these images, mention brand if present. Answer must only just give" +
                    " info of products separated by commas. "
//                + "The first character should be the no. of images, then the remaining response"

        viewModelScope.launch(Dispatchers.IO) {

            val imageRequestBuilder = coil.request.ImageRequest.Builder(app)
            val imageLoader = ImageLoader.Builder(app).build()

            val bitmaps = selectedImages.mapNotNull {
                val imageRequest = imageRequestBuilder
                    .data(it)
                    // Scale the image down to 768px for faster uploads
                    .size(size = 768)
                    .precision(Precision.EXACT)
                    .build()
                try {
                    val result = imageLoader.execute(imageRequest)
                    if (result is SuccessResult) {
                        return@mapNotNull (result.drawable as BitmapDrawable).bitmap
                    } else {
                        return@mapNotNull null
                    }
                } catch (e: Exception) {
                    return@mapNotNull null
                }
            }

            try {
                val inputContent = content {
                    for (bitmap in bitmaps) {
                        image(bitmap)
                    }
                    text(prompt)
                }


                val outputContent = generativeModel.generateContentStream(inputContent)
                    .toList() // Collects all responses
                    .joinToString("") { it.text.toString() } // Joins responses into a single string

                // Update the UI state with the final output
                _uiState.value = PhotoReasoningUiState.GeminiSuccess(outputContent)
            } catch (e: Exception) {
                _uiState.value = PhotoReasoningUiState.Error(e.localizedMessage ?: "")
            }
        }
    }

    private fun generateImage(prompt: String, onImageUrlGenerated: (String?) -> Unit) {
        val request = ImageRequest(prompt)
        RetrofitInstance.api.generateImage(request).enqueue(object : Callback<ImageResponse> {
            override fun onResponse(call: Call<ImageResponse>, response: Response<ImageResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val imageUrl = response.body()!!.image_url
                    Log.d("ImageGeneration", "Image URL: $imageUrl")
                    onImageUrlGenerated(imageUrl) // Pass the image URL to the callback
                } else {
                    Log.e("ImageGeneration", "Error: ${response.code()}")
                    onImageUrlGenerated(null) // Pass null if there's an error
                }
            }

            override fun onFailure(call: Call<ImageResponse>, t: Throwable) {
                Log.e("ImageGeneration", "Failure: ${t.message}")
                onImageUrlGenerated(null) // Pass null on failure
            }
        })
    }
}

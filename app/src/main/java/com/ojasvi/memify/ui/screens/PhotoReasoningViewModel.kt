package com.ojasvi.memify.ui.screens

import android.app.Application
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.ojasvi.memify.BuildConfig.API_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

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

    fun setLoadingUiState() {
        _uiState.value = PhotoReasoningUiState.Loading
    }

    fun setLoadedUiState(outputText: String) {
        _uiState.value = PhotoReasoningUiState.Success(outputText)
    }

    fun reason(
        selectedImages: List<Uri>
    ) {

        _uiState.value = PhotoReasoningUiState.Loading
        val prompt ="Meticulously tell what products are present in these images, mention brand if present. Answer must only just give" +
                " info of products separated by commas. "
//                + "The first character should be the no. of images, then the remaining response"

        viewModelScope.launch(Dispatchers.IO) {

            val imageRequestBuilder = ImageRequest.Builder(app)
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
                _uiState.value = PhotoReasoningUiState.Success(outputContent)
            } catch (e: Exception) {
                _uiState.value = PhotoReasoningUiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}

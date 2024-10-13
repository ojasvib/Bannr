package com.ojasvi.memify.ui.screens

import ImageRequest
import ImageResponse
import RetrofitInstance
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.ojasvi.memify.R
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.CaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt

@Composable
fun PhotoReasoningScreen(
    modifier: Modifier = Modifier.navigationBarsPadding(),
    viewModel: PhotoReasoningViewModel = viewModel()
) {

    val photoReasoningUiState by viewModel.uiState.collectAsState()

    PhotoReasoningContents(
        uiState = photoReasoningUiState,
        onImageAdded = viewModel::reason,
        onUpdateImage = viewModel::resetToInitialUiState,
        onGeminiResponseReceived = viewModel::setLoadingUiState,
        onImagenResponseReceived = { viewModel.setLoadedUiState("") }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PhotoReasoningContents(
    uiState: PhotoReasoningUiState = PhotoReasoningUiState.Loading,
    onImageAdded: (List<Uri>) -> Unit,
    onUpdateImage: () -> Unit,
    onGeminiResponseReceived: () -> Unit,
    onImagenResponseReceived: (String) -> Unit,
) {
    var imageUri by rememberSaveable { mutableStateOf<List<Uri>>(listOf()) }
    var bannerUri by rememberSaveable { mutableStateOf("") }

    var prompt by rememberSaveable { mutableStateOf("") }
    val captureController = rememberCaptureController()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var rotationAngle by remember { mutableFloatStateOf(0f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        fun resetState() {
            onUpdateImage()
            scale = 1f
            rotationAngle = 0f
            offsetX = 0f
            offsetY = 0f
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .capturable(captureController)
        ) {
            if(imageUri.isNotEmpty())
                SourceImage(imageUri[0], captureController, bannerUri)
            OverlayTextCard(uiState = uiState,
                scale,
                rotationAngle,
                offsetX,
                offsetY,
                onDrag = { xDrag, yDrag ->
                    offsetX += xDrag
                    offsetY += yDrag
                },
                onPinchOrRotate = { zoom, rotation ->
                    // Handle two-finger zoom
                    scale *= zoom
                    // Handle two-finger rotation
                    rotationAngle += rotation
                },
                prompt= prompt,
                onStartGeneratingBanner = {onGeminiResponseReceived()}
                ,
                onGenerateBanner = {imageUris -> bannerUri = imageUris
                },
                onBannerGenerated = {onImagenResponseReceived(bannerUri)}
            )
        }

        InputField(
            prompt = prompt,
            onPromptChanged = { prompt = it },
            onSendClicked = {  },
            onImagesSelected = { selectedImageUris ->
                resetState()
                imageUri = selectedImageUris
                onImageAdded(imageUri)
            }
        )
    }
}

@Composable
private fun OverlayTextCard(
    uiState: PhotoReasoningUiState,
    scale: Float,
    rotationAngle: Float,
    offsetX: Float,
    offsetY: Float,
    onDrag: (Float, Float) -> Unit,
    onPinchOrRotate: (Float, Float) -> Unit,
    prompt: String,
    onGenerateBanner: (String) -> Unit,
    onStartGeneratingBanner: () -> Unit,
    onBannerGenerated: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {

            PhotoReasoningUiState.Initial -> {}

            PhotoReasoningUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is PhotoReasoningUiState.Success -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset {
                            IntOffset(
                                offsetX.roundToInt(),
                                offsetY.roundToInt()
                            )
                        } // Move the button
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            rotationZ = rotationAngle
                        ) //rotate and zoom
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                // Handle one-finger drag (by checking pan for drag amount)
                                if (pan != androidx.compose.ui.geometry.Offset.Zero) {
                                    onDrag(scale * pan.x, scale * pan.y)
                                }
                                onPinchOrRotate(zoom, rotation)
                            }
                        }
                ) {

                        var textFieldValue by remember { mutableStateOf(uiState.outputText) }
                        onStartGeneratingBanner()
                        LaunchedEffect(uiState.outputText) {
                            if(uiState.outputText.isNotBlank())
                                textFieldValue = uiState.outputText

//                            generateImage(uiState.outputText+" Use preceding information about number of products " +
//                                    "followed by the products to generate a visually appealing promotional banner." +
//                                    "Include the following customization details: "+prompt+"The banner should highlight the " +
//                                    "listed products, using bold text for the promotion and a suitable call-to-action like " +
//                                    "'Celebrate in Style!'. Include decorative elements related to the theme (such as 'diya " +
//                                    "lamps, sparkles, and floral patterns for Diwali') to enhance the overall design"
//                            ){imageUrl ->


//                              generateImage("A promo banner for "+uiState.outputText+"Carefully Add a catchy banner text"+prompt)  {imageUrl ->
                            if(uiState.outputText.isNotBlank())
                            {
                                var promppp = "Create Promo Banner that has products ${uiState.outputText} and has an awesome catchy headline to attract customers"
                                if(prompt.isNotBlank()){
                                    promppp+= "Include the following customization details: $prompt"
                                }
                            generateImage(promppp
                            ) { imageUrl ->
                                if (imageUrl != null) {
                                    onGenerateBanner(imageUrl)
                                }
                            }
                            }
                        }
                        onBannerGenerated()
                }
            }

            is PhotoReasoningUiState.Error -> {
                Card(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(all = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InputField(
    prompt: String,
    onPromptChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onImagesSelected: (List<Uri>) -> Unit, // Updated to handle multiple images
) {
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { selectedImageUri ->
        selectedImageUri.let { onImagesSelected(it) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 14.dp)
                .navigationBarsPadding()
        ) {
            IconButton(
                onClick = {
                    pickMedia.launch(
                        "image/*"
                    )
                },
                modifier = Modifier
                    .padding(all = 4.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.add_image),
                )
            }
            OutlinedTextField(
                value = prompt,
                label = { Text(stringResource(R.string.reason_label)) },
                placeholder = { Text(stringResource(R.string.reason_hint)) },
                onValueChange = { onPromptChanged(it) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
            )
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

@Composable
fun ShowGeneratedImage(imageUrl: String?) {
    if (imageUrl != null) {
        Image(
            painter = rememberImagePainter(imageUrl),
            contentDescription = "Generated Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Set the height according to your design
                .padding(8.dp)
        )
    } else {
        Text("No image available")
    }
}

@OptIn(ExperimentalComposeApi::class)
@Composable
private fun SourceImage(imageUri: Uri, captureController: CaptureController, bannerUri: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Box {
        ShowGeneratedImage(bannerUri)
    }
}
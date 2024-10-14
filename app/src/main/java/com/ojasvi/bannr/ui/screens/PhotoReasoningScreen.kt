package com.ojasvi.bannr.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import com.ojasvi.bannr.R

@Composable
fun PhotoReasoningScreen(
    modifier: Modifier = Modifier.navigationBarsPadding(),
    viewModel: PhotoReasoningViewModel = viewModel()
) {
    var productImagesUri by rememberSaveable { mutableStateOf<List<Uri>>(listOf()) }

    val photoReasoningUiState by viewModel.uiState.collectAsState()

    PhotoReasoningContents(
        uiState = photoReasoningUiState,
        onImageAdded = { selectedImageUris ->
            viewModel.resetToInitialUiState()
            productImagesUri = selectedImageUris
        },
        onSendClicked = { viewModel.reasonWithGemini(productImagesUri) },
        onGeminiResponseReceived = viewModel::generatePromoBanner,
        productImagesUri = productImagesUri
    )
}

@Composable
fun PhotoReasoningContents(
    uiState: PhotoReasoningUiState = PhotoReasoningUiState.Loading,
    onImageAdded: (List<Uri>) -> Unit,
    onGeminiResponseReceived: (String) -> Unit,
    onSendClicked: () -> Unit,
    productImagesUri: List<Uri>
) {

    var userPrompt by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {


        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Banner(uiState = uiState,
                onStartGeneratingBanner = {
                    onGeminiResponseReceived(userPrompt)
                }
            )
        }

        InputField(
            prompt = userPrompt,
            onPromptChanged = { userPrompt = it },
            onImagesSelected = { selectedImageUris ->
                onImageAdded(selectedImageUris)
            },
            onSendClicked = { onSendClicked() },
            productImagesUri = productImagesUri
        )
    }
}

@Composable
private fun Banner(
    uiState: PhotoReasoningUiState,
    onStartGeneratingBanner: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {

            PhotoReasoningUiState.Initial -> {}

            PhotoReasoningUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is PhotoReasoningUiState.GeminiSuccess -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                LaunchedEffect(Unit) {
                    onStartGeneratingBanner()
                }
            }

            is PhotoReasoningUiState.ImagenSuccess -> {
                Image(
                    painter = rememberImagePainter(uiState.bannerLink),
                    contentDescription = "Generated Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                )
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
    onImagesSelected: (List<Uri>) -> Unit,
    productImagesUri: List<Uri>// Updated to handle multiple images
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
        Column {
            LazyRow(
                modifier = Modifier.padding(all = 8.dp)
            ) {
                items(productImagesUri) { imageUri ->
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(4.dp)
                            .requiredSize(72.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
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
                TextButton(
                    onClick = { onSendClicked() },
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.action_go))
                }
            }

        }
    }
}



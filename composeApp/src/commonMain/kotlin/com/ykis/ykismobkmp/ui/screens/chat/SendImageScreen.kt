package com.ykis.ykismobkmp.ui.screens.chat


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType.Companion.Uri
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.mob.ui.components.ZoomableImage
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

/**
 * [SendImageScreen] — screen for confirming sending an image or document.
 * Implemented as a Voyager Screen to support Mac/Android.
 */
class SendImageScreen(
  private val imagePath: String,
  private val address: String
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val chatViewModel = koinScreenModel<ChatViewModel>()

    SendImageContent(
      imagePath = imagePath,
      address = address,
      navigateBack = { navigator.pop() },
      chatViewModel = chatViewModel
    )
  }
}

@Composable
fun SendImageContent(
  imagePath: String,
  address: String,
  navigateBack: () -> Unit,
  chatViewModel: ChatViewModel
) {
  val className = "SendImageScreen"

  // Subscribe to ViewModel states
  val aiAssistantResponse by chatViewModel.assistantResponse.collectAsState()
  val messageText by chatViewModel.messageText.collectAsState()
  val isLoadingAfterSending by chatViewModel.isLoadingAfterSending.collectAsState()

  // Content type detection (KMP version)
  val isImage = remember(imagePath) {
    val path = imagePath.lowercase()
    path.endsWith(".jpg") || path.endsWith(".jpeg") ||
      path.endsWith(".png") || path.contains("camera") || path.contains("image")
  }

  Log.d("YkisLog", "[$className.Content]: [TYPE_CHECK] Path: $imagePath | isImage: $isImage")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentAlignment = Alignment.Center
    ) {
      // 2. Conditional display
      if (isImage) {
        Log.d("YkisLog", "[$className.Content]: Rendering as IMAGE")
        // In KMP, we use the image loader by path
        ZoomableImage(imagePath = imagePath)
      } else {
        Log.d("YkisLog", "[$className.Content]: Rendering as DOCUMENT")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = "Документ готовий до відправки",
            style = MaterialTheme.typography.titleMedium
          )
        }
      }

      IconButton(
        modifier = Modifier
          .padding(8.dp)
          .align(Alignment.TopStart),
        onClick = {
          Log.d("YkisLog", "[$className.Content]: Back pressed")
          navigateBack()
        }
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
      }
    }

    // AI (Gemini) suggestion block
    AnimatedVisibility(visible = !aiAssistantResponse.isNullOrBlank()) {
      Surface(
        modifier = Modifier
          .padding(8.dp)
          .clickable {
            chatViewModel.onMessageTextChanged(aiAssistantResponse!!)
            chatViewModel.clearAiSuggestion()
          },
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = aiAssistantResponse ?: "",
          modifier = Modifier.padding(12.dp),
          style = MaterialTheme.typography.bodyMedium
        )
      }
    }

    // Message input field
    ComposeMessageBox(
      text = messageText,
      onTextChanged = { chatViewModel.onMessageTextChanged(it) },
      onSent = {
        Log.d("YkisLog", "[$className.onSent]: Send clicked")
        chatViewModel.uploadFileAndSendMessage(
          imagePath = imagePath,
          onComplete = { navigateBack() }
        )
      },
      onImageSent = {},
      onAiClick = {
        if (isImage) {
          Log.d("YkisLog", "[$className.onAi]: Starting Gemini Analysis")
          chatViewModel.analyzePhotoWithGemini(imagePath, address)
        } else {
          Log.d("YkisLog", "[$className.onAi]: AI only works with images")
        }
      },
      showAttachIcon = false,
      onCameraClick = {},
      isLoading = isLoadingAfterSending,
      canSend = messageText.isNotBlank() || isImage
    )
  }
}



@Preview(showBackground = true)
@Composable
private fun PreviewSendImageScreen() {
  YkisPAMTheme {
    SendImageScreen(
      imageUri = Uri.EMPTY,
      messageText = "",
      onMessageTextChanged = {},
      navigateBack = {},
      address = "",
      onSent = {},
      isLoadingAfterSending = false,
      chatViewModel = viewModel()
    )
  }
}

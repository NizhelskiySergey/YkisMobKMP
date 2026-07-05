package com.ykis.ykismobkmp.ui.screens.chat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import org.koin.compose.koinInject


private const val tag = "SendImageScreen"

class SendImageScreen(
  private val imagePath: String,
  private val address: String,
  private val chatId: String? = null, // Передаем ID чата напрямую
  private val fileName: String? = null
) : Screen {
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val chatScreenModel = koinInject<ChatScreenModel>()

    SendImageContent(
      imagePath = imagePath,
      address = address,
      chatId = chatId,
      fileName = fileName,
      navigateBack = {
        navigator.pop()
      },
      chatScreenModel = chatScreenModel
    )
  }
}

@Composable
fun SendImageContent(
  imagePath: String,
  address: String,
  chatId: String?,
  fileName: String?,
  navigateBack: () -> Unit,
  chatScreenModel: ChatScreenModel
) {
  val aiAssistantResponse by chatScreenModel.assistantResponse.collectAsState()
  val messageText by chatScreenModel.messageText.collectAsState()
  val isLoadingAfterSending by chatScreenModel.isLoadingAfterSending.collectAsState()
  val isAssistantLoading by chatScreenModel.isAssistantLoading.collectAsState()

  // ВИПРАВЛЕНО: Розширене визначення зображення для підтримки Blob/Web
  val isImage = remember(imagePath, fileName) {
    val path = imagePath.lowercase()
    val name = fileName?.lowercase() ?: ""
    path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || 
      path.contains("image") || path.startsWith("blob:") ||
      name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
  }

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
        .fillMaxWidth()
        .background(Color.Black.copy(alpha = 0.05f)),
      contentAlignment = Alignment.Center
    ) {
      if (isImage) {
        // ВИПРАВЛЕНО: Використовуємо надійний спосіб центрування без розтягування
        AsyncImage(
          model = imagePath,
          contentDescription = "Preview",
          contentScale = ContentScale.Fit,
          alignment = Alignment.Center,
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        )
        
        // ДОДАНО: Центровий лоадер поки ШІ думає
        if (isAssistantLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("ШІ аналізує фото...", color = Color.White)
                }
            }
        }
      } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = fileName ?: "Документ готовий до відправки",
            style = MaterialTheme.typography.titleMedium
          )
        }
      }

      IconButton(
        modifier = Modifier
          .padding(8.dp)
          .align(Alignment.TopStart)
          .background(Color.Black.copy(alpha = 0.3f), CircleShape),
        onClick = {
          navigateBack()
        }
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
      }
    }

    AnimatedVisibility(visible = !aiAssistantResponse.isNullOrBlank()) {
      Surface(
        modifier = Modifier
          .padding(8.dp)
          .clickable {
            chatScreenModel.onMessageTextChanged(aiAssistantResponse!!)
            chatScreenModel.clearAiSuggestion()
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

    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val apartmentLiveUiState by apartmentScreenModel.uiState.collectAsState()
    val targetUser by chatScreenModel.selectedUser.collectAsState()

    ComposeMessageBox(
      text = messageText,
      onTextChanged = { chatScreenModel.onMessageTextChanged(it) },
      onSent = {
        val myUid = apartmentLiveUiState.uid ?: ""
        val role = apartmentLiveUiState.userRole
        val user = targetUser

        val curAddrId = if (role == UserRole.StandardUser) apartmentLiveUiState.addressId else (user?.addressId ?: 0L)
        val curOsbbId = if (role == UserRole.StandardUser) (apartmentLiveUiState.osmdId) else apartmentLiveUiState.osbbId

        println("[YkisLogKMP]: [SEND_IMAGE_CLICK] UID: $myUid, Role: $role, AddrID: $curAddrId, OsbbID: $curOsbbId, ChatID: $chatId")

        val (displayName, displayAddr) = if (role == UserRole.StandardUser) {
            val surname = apartmentLiveUiState.nanim ?: ""
            val cleanSurname = if (surname.isNotBlank() && surname != "Мешканець") surname else "Жилець"
            cleanSurname to apartmentLiveUiState.address
        } else {
            apartmentLiveUiState.osbb to " "
        }

        chatScreenModel.uploadFileAndSendMessage(
          filePath = imagePath,
          chatId = chatId,
          senderUid = myUid,
          senderDisplayedName = displayName,
          senderLogoUrl = apartmentLiveUiState.photoUrl,
          senderAddress = displayAddr,
          addressId = curAddrId,
          osbbId = curOsbbId,
          role = role,
          recipientTokens = user?.tokens ?: emptyList(),
          onComplete = {
            navigateBack()
          }
        )
      },
      onImageSent = { _, _ -> },
      onCameraClick = {},
      showAttachIcon = false,
      isLoading = isLoadingAfterSending || isAssistantLoading,
      canSend = messageText.isNotBlank() || isImage
    )
  }
}

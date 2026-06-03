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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import org.koin.compose.koinInject


private const val tag = "SendImageScreen"

/**
 * [SendImageScreen] — Кроссплатформенный Voyager-экран полноэкранного превью и отправки фото/документов.
 */
class SendImageScreen(
  private val imagePath: String,
  private val address: String
) : Screen {
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val chatScreenModel = koinInject<ChatScreenModel>()

    SendImageContent(
      imagePath = imagePath,
      address = address,
      navigateBack = {
        println("[$tag.Content]: Повернення до стрічки повідомлень. Нативний Voyager pop.")
        navigator.pop()
      },
      chatScreenModel = chatScreenModel
    )
  }
}

/**
 * [SendImageContent] — Stateless-компоновщик графического холста отправки медиафайлов коммунальных заявок.
 */
@Composable
fun SendImageContent(
  imagePath: String,
  address: String,
  navigateBack: () -> Unit,
  chatScreenModel: ChatScreenModel
) {
  val methodName = "SendImageContent"
  val aiAssistantResponse by chatScreenModel.assistantResponse.collectAsState()
  val messageText by chatScreenModel.messageText.collectAsState()
  val isLoadingAfterSending by chatScreenModel.isLoadingAfterSending.collectAsState()

  val isImage = remember(imagePath) {
    val path = imagePath.lowercase()
    path.endsWith(".jpg") || path.endsWith(".jpeg") ||
      path.endsWith(".png") || path.contains("camera") || path.contains("image")
  }

  println("[$tag.$methodName]: [TYPE_CHECK] Шлях: $imagePath | Полотно є картинкою: $isImage")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding() // Мягко поднимает текстовый MessageBox над клавиатурой смартфона
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentAlignment = Alignment.Center
    ) {
      if (isImage) {
        println("[$tag.$methodName]: Рендеринг зображення лічильника/акту через Coil 3")
        AsyncImage(
          model = imagePath,
          contentDescription = "Прев'ю фото поломки перед відправкою",
          modifier = Modifier.fillMaxSize()
        )
      } else {
        println("[$tag.$methodName]: Рендеринг блоку прев'ю документа")
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
          .align(Alignment.TopStart)
          .background(Color.Black.copy(alpha = 0.3f), CircleShape),
        onClick = {
          println("[$tag.$methodName]: Натиснуто стрілку назад")
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
            chatScreenModel.onMessageTextChanged(aiAssistantResponse !!)
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

    // Извлекаем BaseUIState напрямую из легитимного КМР-источника ApartmentScreenModel
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val apartmentLiveUiState by apartmentScreenModel.uiState.collectAsState()

    // Реактивно подписываемся на выбранного админом/жильцом оппонента чата
    val targetUser by chatScreenModel.selectedUser.collectAsState()

    ComposeMessageBox(
      text = messageText,
      onTextChanged = { chatScreenModel.onMessageTextChanged(it) },
      onSent = {
        println("[$tag.onSent]: Запуск атомарного завантаження медіафайлу в хмару та відправки повідомлення")

        val myUid = apartmentLiveUiState.uid ?: ""
        val targetUid = if (apartmentLiveUiState.userRole == UserRole.StandardUser) myUid else targetUser.uid

        val curAddrId = if (apartmentLiveUiState.userRole == UserRole.StandardUser)
          apartmentLiveUiState.addressId else targetUser.addressId

        val curAddr = if (apartmentLiveUiState.userRole == UserRole.StandardUser)
          (apartmentLiveUiState.address ?: "м. Южне") else (targetUser.displayName ?: "Абонент")

        // Вызываем запечатанный метод вьюмодели с передачей сквозных Long-идентификаторов ЮКІС СУБД!
        chatScreenModel.uploadFileAndSendMessage(
          chatUid = targetUid,
          senderUid = myUid,
          senderDisplayedName = apartmentLiveUiState.displayName ?: "Користувач",
          senderLogoUrl = apartmentLiveUiState.photoUrl,
          senderAddress = curAddr,
          addressId = curAddrId, // Сквозной Long тип
          osbbId = apartmentLiveUiState.osmdId ?: apartmentLiveUiState.osbbId ?: 0L, // Сквозной Long тип
          role = apartmentLiveUiState.userRole,
          recipientTokens = targetUser.tokens ?: emptyList(),
          onComplete = {
            println("[$tag.onSent]: Завантаження завершено. Повернення в стрічку повідомлень чату.")
            navigateBack()
          }
        )
      },
      onImageSent = { clickedImagePath ->
        println("[$tag.onImageSent]: Повторна генерація кадру: $clickedImagePath")
      },
      onCameraClick = {},
      onAiClick = {
        if (isImage) {
          println("[$tag.onAi]: Запуск КМР Gemini комп'ютерного зору для розпізнавання фото лічильника Водоканалу.")
          chatScreenModel.analyzePhotoWithGemini(imagePath, address)
        } else {
          println("[$tag.onAi_WARN]: Аналіз Gemini заблоковано. Працює виключно з фотографіями.")
        }
      },
      showAttachIcon = false,
      isLoading = isLoadingAfterSending,
      canSend = messageText.isNotBlank() || isImage
    )
  }
}




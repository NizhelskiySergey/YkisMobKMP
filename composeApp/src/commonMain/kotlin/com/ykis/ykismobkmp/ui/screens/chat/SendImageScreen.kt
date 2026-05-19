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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import org.koin.compose.koinInject


private const val tag = "SendImageScreen"

/**
 * [SendImageScreen] — Кроссплатформенный Voyager-экран подтверждения отправки изображений или документов.
 * ИСПРАВЛЕНО: Платформозависимый ChatViewModel заменен на зафиксированный КМР-класс ChatScreenModel через koinInject.
 */
class SendImageScreen(
  private val imagePath: String,
  private val address: String
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Нативная КМР инжекция ScreenModel вместо Android Lifecycle-зависимых вьюмоделей
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
 * [SendImageContent] — Декларативная Stateless-верстка превью-интерфейса перед отправкой медиа в ОСМД.
 */
@Composable
fun SendImageContent(
  imagePath: String,
  address: String,
  navigateBack: () -> Unit,
  chatScreenModel: ChatScreenModel
) {
  val methodName = "SendImageContent"

  // Реактивно подписываемся на фоновые потоки стейтов Gemini ИИ и текстового поля из ChatScreenModel
  val aiAssistantResponse by chatScreenModel.assistantResponse.collectAsState()
  val messageText by chatScreenModel.messageText.collectAsState()
  val isLoadingAfterSending by chatScreenModel.isLoadingAfterSending.collectAsState()

  // Нативное КМР определение типа контента по расширению строкового файла ссылки
  val isImage = remember(imagePath) {
    val path = imagePath.lowercase()
    path.endsWith(".jpg") || path.endsWith(".jpeg") ||
      path.endsWith(".png") || path.contains("camera") || path.contains("image")
  }

  println("[$tag.$methodName]: [TYPE_CHECK] Path: $imagePath | isImage: $isImage")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding() // Автоматический КМР-отступ экрана при поднятии нативной клавиатуры смартфона
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentAlignment = Alignment.Center
    ) {
      if (isImage) {
        println("[$tag.$methodName]: Rendering as IMAGE via Coil 3")
        // ИСПРАВЛЕНО: ZoomableImage с Android Uri заменен на Coil 3 AsyncImage, стабильный на Mac/iOS
        AsyncImage(
          model = imagePath,
          contentDescription = "Превью фото поломки перед отправкой в ОСМД",
          modifier = Modifier.fillMaxSize()
        )
      } else {
        println("[$tag.$methodName]: Rendering as DOCUMENT bubble")
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
          println("[$tag.$methodName]: Нажата кнопка возврата назад")
          navigateBack()
        }
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
      }
    }

    // Блок подсказок ИИ ассистента (Интеграция результатов анализа фотографии Gemini AI)
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
    val apartmentUiState by apartmentScreenModel.uiState.collectAsState()
    val chatUiState by chatScreenModel.uiState.collectAsState() // Твой стейт чатов (токены, роли, uid)

    ComposeMessageBox(
      text = messageText,
      onTextChanged = { chatScreenModel.onMessageTextChanged(it) },
      onSent = {
        println("[$tag.onSent]: Запуск атомарної завантаження файлу в хмару та відправки повідомлення")

        // РЕШЕНИЕ: Передаем все 10 обязательных параметров-контекстов, вычитанных из КМР-стейтов!
        chatScreenModel.uploadFileAndSendMessage(
          chatUid = chatUiState.currentChatUid ?: "",
          senderUid = chatUiState.uid ?: "",
          senderDisplayedName = chatUiState.displayName?: "Абонент",
          senderLogoUrl = chatUiState.opponentLogoUrl,
          senderAddress = apartmentUiState.address,
          addressId = apartmentUiState.addressId, // Сквозной Long-тип под требования SQLDelight
          osbbId = apartmentUiState.osbbId,       // Сквозной Long-тип под требования SQLDelight
          role = chatUiState.userRole,
          recipientTokens = chatUiState.activeRecipientFcmTokens ?: emptyList(),
          onComplete = {
            println("[$tag.onSent]: Коллбэк завершения. Возврат в ленту чата.")
            navigateBack()
          }
        )
      },
      onImageSent = { clickedImagePath ->
        println("[$tag.onImageSent]: Повторна відправка кадру: $clickedImagePath")
      },
      onCameraClick = {},
      onAiClick = {
        if (isImage) {
          println("[$tag.onAi]: Ініціалізація КМР Gemini AI аналізу фотографії ЖКХ фонду")
          chatScreenModel.analyzePhotoWithGemini(imagePath, address)
        } else {
          println("[$tag.onAi]: Аналіз Gemini ІИ заблоковано. Працює тільки з фото.")
        }
      },
      showAttachIcon = false,
      isLoading = isLoadingAfterSending,
      canSend = messageText.isNotBlank() || isImage
    )


  }
}

/**
 * ИСПРАВЛЕНО: Из превью полностью удалены ложные Android-зависимости.
 * Переведено на КМР-совместимый Preview холст JetBrains.
 */
@Preview
@Composable
private fun PreviewSendImageScreen() {
  YkisPAMTheme {
    SendImageContent(
      imagePath = "demo.jpg",
      address = "ул. Ленина 10, кв. 5",
      navigateBack = {},
      chatScreenModel = koinInject<ChatScreenModel>()
    )
  }
}


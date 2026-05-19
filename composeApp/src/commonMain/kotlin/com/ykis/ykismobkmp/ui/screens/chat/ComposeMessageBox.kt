package com.ykis.ykismobkmp.ui.screens.chat

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log

private const val className = "ComposeMessageBox"

/**
 * [ComposeMessageBox] — мультиплатформенное поле ввода сообщений.
 * Поддерживает текст, AI-помощника, вложения и камеру.
 */
@Composable
fun ComposeMessageBox(
  onSent: () -> Unit,
  onImageSent: (String) -> Unit, // В KMP передаем путь String вместо Uri
  onCameraClick: () -> Unit,
  onAiClick: () -> Unit,
  text: String,
  onTextChanged: (String) -> Unit,
  showAttachIcon: Boolean = true,
  isLoading: Boolean,
  canSend: Boolean
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)

  // В KMP для выбора файлов на Mac/Android используем кроссплатформенный FilePicker
  // Здесь мы вызываем лямбду, которая инициирует выбор в платформенном слое
  val triggerFilePicker = {
    Log.d("YkisLog", "[$className.FilePicker]: Triggering platform file picker")
    // Логика выбора файла вынесена в платформенный репозиторий или expect/actual
    // Для примера вызываем заглушку, которую ты заполнишь выбранным путем
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight()
      .padding(horizontal = 4.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
  ) {
    if (showAttachIcon) {
      // Кнопка ИИ
      IconButton(onClick = {
        Log.d("YkisLog", "[$className.onAiClick]: Assistant requested")
        onAiClick()
      }, enabled = !isLoading) {
        Icon(Icons.Default.SmartToy, null, tint = MaterialTheme.colorScheme.primary)
      }

      // Кнопка вложений
      IconButton(onClick = {
        Log.d("YkisLog", "[$className.onAttach]: Attachment clicked")
        triggerFilePicker()
      }, enabled = !isLoading) {
        Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Прикріпити")
      }

      // Кнопка камеры
      IconButton(onClick = {
        Log.d("YkisLog", "[$className.onCamera]: Camera clicked")
        onCameraClick()
      }, enabled = !isLoading) {
        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Камера")
      }
    }

    // Поле ввода текста
    BasicTextField(
      value = text,
      onValueChange = {
        onTextChanged(it)
      },
      modifier = Modifier.weight(1f),
      textStyle = textStyle,
      decorationBox = { innerTextField ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
          if (text.isEmpty()) {
            Text(
              text = "Повідомлення",
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
          }
          innerTextField()
        }
      }
    )

    // Кнопка отправки или индикатор загрузки
    Crossfade(isLoading, label = "send_state_fade") { loading ->
      if (loading) {
        CircularProgressIndicator(
          modifier = Modifier
            .size(48.dp)
            .padding(12.dp),
          strokeWidth = 3.dp
        )
      } else {
        IconButton(
          onClick = {
            Log.d("YkisLog", "[$className.onSent]: Send clicked. Text length: ${text.length}")
            onSent()
            keyboardController?.hide()
          },
          enabled = canSend
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Відправити",
            tint = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
          )
        }
      }
    }
  }
}




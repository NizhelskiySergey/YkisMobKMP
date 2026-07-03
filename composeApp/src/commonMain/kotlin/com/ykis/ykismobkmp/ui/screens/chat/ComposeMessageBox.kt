package com.ykis.ykismobkmp.ui.screens.chat

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.FilePicker
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.message_placeholder

private const val className = "ComposeMessageBox"

/**
 * [ComposeMessageBox] — мультиплатформенное поле ввода сообщений.
 */
@Composable
fun ComposeMessageBox(
  onSent: () -> Unit,
  onImageSent: (String, String?) -> Unit,
  onCameraClick: () -> Unit,
  text: String,
  onTextChanged: (String) -> Unit,
  showAttachIcon: Boolean = true,
  isLoading: Boolean,
  canSend: Boolean,
  filePicker: FilePicker? = null // ДОДАНО: Прямий доступ до пікера
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)

  // Добавляем скролл-стейт для удержания фокуса при многострочном вводе коммунальных заявок
  val scrollState = rememberScrollState()

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp, vertical = 2.dp),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.Center
  ) {
    if (showAttachIcon) {
      val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)
      
      // Кнопка вкладень
      IconButton(
        onClick = {
          println("[YkisLogKMP.$className.onAttach]: Виклик системного FilePicker.")
          if (filePicker != null) {
              filePicker.pickFile { path, name -> onImageSent(path, name) }
          } else {
              onImageSent("", null)
          }
        },
        enabled = !isLoading
      ) {
        Icon(
          imageVector = Icons.Default.AttachFile, 
          contentDescription = "Прикріпити"
        )
      }

      // Кнопка камери (неактивна для Web)
      IconButton(
        onClick = {
          println("[YkisLogKMP.$className.onCamera]: Ініціалізація апаратної камери.")
          onCameraClick()
        },
        enabled = !isLoading && !isWeb
      ) {
        Icon(
          imageVector = Icons.Default.CameraAlt, 
          contentDescription = "Камера",
          tint = if (!isWeb) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
      }
    }

    // Поле ввода текста
    BasicTextField(
      value = text,
      onValueChange = { onTextChanged(it) },
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 4.dp, vertical = 4.dp)
        // ИСПРАВЛЕНО: Заменили на sizeIn(maxHeight = ...) для 100% КМР-компиляции на iOS/Android!
        .sizeIn(maxHeight = 120.dp)
        .verticalScroll(scrollState),
      textStyle = textStyle,
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), // ФИКС: Явный цвет курсора для Web
      decorationBox = { innerTextField ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          if (text.isEmpty()) {
            Text(
              text = stringResource(Res.string.message_placeholder),
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
              style = MaterialTheme.typography.bodyMedium
            )
          }
          innerTextField()
        }
      }
    )

    // Кнопка отправки или индикатор загрузки пакета Firebase
    Crossfade(isLoading, label = "send_state_fade") { loading ->
      if (loading) {
        CircularProgressIndicator(
          modifier = Modifier
            .size(40.dp)
            .padding(8.dp),
          strokeWidth = 2.5.dp
        )
      } else {
        IconButton(
          onClick = {
            println("[YkisLogKMP.$className.onSent]: Натиснуто відправку. Довжина тексту: ${text.length} симв.")
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







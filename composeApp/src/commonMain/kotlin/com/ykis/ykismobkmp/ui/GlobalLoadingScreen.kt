package com.ykis.ykismobkmp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun GlobalLoadingBarrier(isVisible: Boolean) {
  if (isVisible) {
    Dialog(
      onDismissRequest = { /* Не закрываем при клике мимо */ },
      properties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false
      )
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(
          color = MaterialTheme.colorScheme.primary,
          strokeWidth = 4.dp
        )
      }
    }
  }
}

//
//@Composable
//fun GlobalLoadingBarrier(isShowing: Boolean) {
//  if (isShowing) {
//    // Box на весь экран, блокирующий ввод
//    Box(
//      modifier = Modifier
//        .fillMaxSize()
//        .background(Color.Black.copy(alpha = 0.4f)) // Затемнение
//        .pointerInput(Unit) {}, // Блокировка кликов
//      contentAlignment = Alignment.Center
//    ) {
//      Card(
//        shape = RoundedCornerShape(12.dp),
//        elevation = CardDefaults.cardElevation(8.dp)
//      ) {
//        Column(
//          modifier = Modifier.padding(24.dp),
//          horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
//          Spacer(modifier = Modifier.height(16.dp))
//          Text(text = "Загрузка...", style = MaterialTheme.typography.bodyMedium)
//        }
//      }
//    }
//  }
//}

package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import com.ykis.ykismobkmp.ui.screens.appartment.AddApartmentScreen
import com.ykis.ykismobkmp.ui.screens.meter.MainMeterScreen

private const val className = "ScreensRegistry"
object SignUpScreen : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.SignUpScreen]: Маршрутизатор передає управління холсту реєстрації")
    com.ykis.ykismobkmp.ui.screens.auth.SignUpScreen.Content()
  }
}
object VerifyEmailScreen : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.VerifyEmailScreen]: Маршрутизатор передає управління холсту верифікації пошти")
    com.ykis.ykismobkmp.ui.screens.auth.VerifyEmailScreen.Content()
  }
}
object AddApartmentScreen : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.AddApartmentScreen]: Маршрутизатор передає управління живому холсту прив'язки квартири")
    AddApartmentScreen(
      onDrawerClicked = { /* Открытие боковой панели шторки */ },
      closeContentDetail = { /* Закрытие подмодуля привязки */ }
    )
  }
}
/** [SendImageScreenDest] — Экран предосмотра и отправки фото счетчика в Gemini AI. */
object SendImageScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Перегляд ІИ-фотографії лічильника") }
  }
}
/**
 * [CameraScreenDest] — Кроссплатформенный видоискатель камеры.
 * */
object CameraScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Екран камери") }
  }
}
/** [ProfileScreenDest] — Окно персональных данных профиля. */
object ProfileScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Профіль користувача") }
  }
}
/**
 * [BtiScreenDest] — Панель характеристик жилья БТИ.
 * */
object BtiScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Дані БТІ") }
  }
}
/**
 * [FamilyScreenDest] — Список зарегистрированных мешканців квартиры.
 * */
object FamilyScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Склад сім'ї") }
  }
}
/**
  [ChatScreenDest] — Универсальный кроссплатформенный экран чат-комнаты ЮКИС.
 **/
data class ChatScreenDest(
  val chatId: String? = null
) : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.ChatScreenDest]: Отрисовка чату для токена: $chatId")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Стрічка обговорень чату для ID: $chatId")
    }
  }
}

data class InfoApartmentScreenDest(
  val addressId: Long = 0L
) : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.InfoApartmentScreenDest]: Open для ID: $addressId")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Інформація про квартиру ID: $addressId")
    }
  }
}

data class ImageDetailScreenDest(
  val imageUrl: String
) : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.ImageDetailScreenDest]: Open для: $imageUrl")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Деталізація зображення: $imageUrl")
    }
  }
}

data class WebViewScreenDest(
  val link: String
) : Screen {
  @Composable
  override fun Content() {
    val formattedLink = remember(link) { link.replace("*", "/") }
    println("[YkisLogKMP.$className.WebViewScreenDest]: Open Xpay Link: $formattedLink")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Платіжний шлюз Xpay: $formattedLink")
    }
  }
}
object MainMeterScreenDest : Screen {
  @Composable
  override fun Content() {
    println("[YkisLogKMP.$className.MainMeterScreenDest]: Маршрутизатор передає управління головному екрану лічильників")
    // Вызов реального холста из вашего feature-модуля приборов учета
    MainMeterScreen()
  }
}

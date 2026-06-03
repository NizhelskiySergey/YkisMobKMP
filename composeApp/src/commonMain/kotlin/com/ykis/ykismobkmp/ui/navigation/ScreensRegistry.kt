package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.AddApartmentScreen
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.InfoApartmentScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.UserListScreen
import com.ykis.ykismobkmp.ui.screens.ledger.LedgerScreenModel
import com.ykis.ykismobkmp.ui.screens.ledger.MainServiceScreen
import com.ykis.ykismobkmp.ui.screens.meter.MainMeterScreen
import com.ykis.ykismobkmp.ui.components.CameraView
import com.ykis.ykismobkmp.ui.screens.chat.SendImageScreen
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreen
import com.ykis.ykismobkmp.ui.components.CameraView
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.accrued
import ykismobkmp.composeapp.generated.resources.chat
import ykismobkmp.composeapp.generated.resources.info
import ykismobkmp.composeapp.generated.resources.meters
import ykismobkmp.composeapp.generated.resources.settings

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
object SendImageScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Перегляд ІИ-фотографії лічильника") }
  }
}
object CameraScreenDest : Screen {
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val chatScreenModel = koinInject<ChatScreenModel>()
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val baseUIState by apartmentScreenModel.uiState.collectAsState()

    println("[YkisLogKMP.ScreensRegistry.CameraScreen]: Запуск нативного компонента камери")
    
    CameraView(
      onImageCaptured = { path ->
        println("[YkisLogKMP.ScreensRegistry.CameraScreen]: Фото зафиксировано: $path")
        chatScreenModel.setSelectedImagePath(path)
        
        // Автоматически запускаем анализ Gemini для жильцов
        if (baseUIState.userRole == UserRole.StandardUser) {
           chatScreenModel.analyzePhotoWithGemini(path, baseUIState.address)
        }
        
        // Переходим к экрану подтверждения отправки
        navigator.replace(com.ykis.ykismobkmp.ui.screens.chat.SendImageScreen(imagePath = path, address = baseUIState.address))
      },
      onBack = {
        println("[YkisLogKMP.ScreensRegistry.CameraScreen]: Возврат из камеры без снимка")
        navigator.pop()
      }
    )
  }
}
object ProfileScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Профіль користувача") }
  }
}
object BtiScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Дані БТІ") }
  }
}
object FamilyScreenDest : Screen {
  @Composable
  override fun Content() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Склад сім'ї") }
  }
}
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
  override val key: cafe.adriel.voyager.core.screen.ScreenKey
    get() = "InfoApartmentScreenDest_${addressId}"
  @Composable
  override fun Content() {
    val classNameRegistry = "ScreensRegistry"
    println("[YkisLogKMP.$classNameRegistry.InfoApartmentScreenDest]: Маршрутизатор передає управління ЖИВОМУ холсту БТІ для о/р: $addressId")
    val navigator = cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow
    InfoApartmentScreen(
      onDrawerClicked = {
        println("[YkisLogKMP.$classNameRegistry.InfoApartmentScreenDest]: Клік по бургер-кнопці на екрані БТІ.")
      }
    )
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
    MainMeterScreen()
  }
}
data class MainServiceScreenDest(
  val addressId: Long = 0L
) : Screen {
  override val key: ScreenKey
    get() = "MainServiceScreenDest_${addressId}"
  @Composable
  override fun Content() {
    val classNameRegistry = "ScreensRegistry"
    println("[YkisLogKMP.$classNameRegistry.MainServiceScreenDest]: Маршрутизатор передає управління ЖИВОМУ фінансовому хабу для о/р: $addressId")
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val baseUIState by apartmentScreenModel.uiState.collectAsState()
    MainServiceScreen(
      baseUIState = baseUIState,
      navigationType = LocalNavigationType.current, // Берем тип навигации из стабильного CompositionLocal
      onDrawerClick = {
        println("[YkisLogKMP.$classNameRegistry.MainServiceScreenDest]: Клік по бургер-кнопці на екрані фінансів.")
      }
    )
  }
}

object SettingsScreenDest : cafe.adriel.voyager.core.screen.Screen {
  @Composable
  override fun Content() {
    val classNameRegistry = "ScreensRegistry"
    println("[YkisLogKMP.$classNameRegistry.SettingsScreenDest]: Маршрутизатор передає управління холсту системних налаштувань")
    val navigator = cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow
    SettingsScreen(
      onDrawerClick = {
        println("[YkisLogKMP.$classNameRegistry.SettingsScreenDest]: Запрос закрытия экрана настроек, возврат по стеку.")
        navigator.pop()
      }
    )
  }
}
data class TopLevelDestination(
  val route: String = "",
  val selectedIcon: ImageVector = Icons.Default.Adjust,
  val unselectedIcon: ImageVector = Icons.Default.Adjust,
  val labelId: StringResource,
  val alwaysVisible: Boolean
)
fun getChatRoute(role: UserRole): String {
  return if (role == UserRole.StandardUser) "chat_selector" else "chat_user_list"
}
fun getNavDestinations(role: UserRole): List<TopLevelDestination> {
  val chatRoute = getChatRoute(role)
  println("[$className.getNavDestinations]: Расчет дестинаций меню для роли: $role | Чат-маршрут: $chatRoute")

  return listOf(
    TopLevelDestination(
      route = "InfoApartmentScreen",
      selectedIcon = Icons.Filled.Info,
      unselectedIcon = Icons.Outlined.Info,
      labelId = Res.string.info,
      alwaysVisible = false
    ),
    TopLevelDestination(
      route = "MeterScreen",
      selectedIcon = Icons.Default.Opacity,
      unselectedIcon = Icons.Default.ElectricMeter,
      labelId = Res.string.meters,
      alwaysVisible = false
    ),
    TopLevelDestination(
      route = "ServiceListScreen",
      selectedIcon = Icons.Filled.Payments,
      unselectedIcon = Icons.Outlined.Payments,
      labelId = Res.string.accrued,
      alwaysVisible = false
    ),
    TopLevelDestination(
      route = chatRoute,
      selectedIcon = Icons.AutoMirrored.Filled.Chat,
      unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
      labelId = Res.string.chat,
      alwaysVisible = false
    ),
    TopLevelDestination(
      route = "SettingsScreen",
      selectedIcon = Icons.Default.Settings,
      unselectedIcon = Icons.Outlined.Settings,
      labelId = Res.string.settings,
      alwaysVisible = true
    )
  )
}

object RouteRegistry {
  const val SIGN_IN = "SignInScreen"
  const val VERIFY_EMAIL = "VerifyEmailScreen"
  const val SIGN_UP = "SignUpScreen"
  const val ADD_APARTMENT = "AddApartmentScreen"
  const val METER = "MeterScreen"
  const val SERVICE_LIST = "ServiceListScreen"
  const val USER_LIST = "UserListScreen"
  const val SEND_IMAGE = "SendImageScreen"
  const val CAMERA = "CameraScreen"
  const val IMAGE_DETAIL = "ImageDetailScreen"
  const val CHAT = "ChatScreen"
  const val PROFILE = "ProfileScreen"
  const val SETTINGS = "SettingsScreen"
  const val BTI = "BtiScreen"
  const val FAMILY = "FamilyScreen"
  const val INFO_APARTMENT = "InfoApartmentScreen"
  const val WEB_VIEW = "WebViewScreen"
}

object YkisNavConstants {
  const val APARTMENT_SCREEN = "ApartmentScreen"
  const val WATER_SCREEN = "WaterScreen"
  const val SERVICE_DETAIL_SCREEN = "ServiceDetailScreen"
  const val ADDRESS_ID = "addressId"
  const val ADDRESS_DEFAULT_ID = "0"
  const val HOUSE_ID = "houseId"
  const val HOUSE_DEFAULT_ID = "0"
  const val SERVICE = "service"
  const val SERVICE_DEFAULT = "1"
  const val SERVICE_NAME = "serviceName"
  const val SERVICE_DEFAULT_NAME = "ОСББ"
  const val ADDRESS = "address"
  const val ADDRESS_DEFAULT = "адреса"
  const val ADDRESS_ID_ARG = "?$ADDRESS_ID={$ADDRESS_ID}"
  const val FLAT_ARG = "?$ADDRESS_ID={$ADDRESS_ID},$ADDRESS={$ADDRESS}"
  const val SERVICE_ARG = "?$ADDRESS_ID={$ADDRESS_ID},$ADDRESS={$ADDRESS},$HOUSE_ID={$HOUSE_ID},$SERVICE={$SERVICE},$SERVICE_NAME={$SERVICE_NAME}"
}

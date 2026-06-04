package com.ykis.ykismobkmp.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.core.utils.formatDateFull
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.navigation.CameraScreenDest
import com.ykis.ykismobkmp.ui.navigation.ImageDetailScreenDest
import com.ykis.ykismobkmp.ui.navigation.LocalContentType
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.navigation.SendImageScreenDest
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.core.utils.rememberFilePicker
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.apply_suggestion
import ykismobkmp.composeapp.generated.resources.cancel
import ykismobkmp.composeapp.generated.resources.delete_for_everyone
import ykismobkmp.composeapp.generated.resources.delete_for_me
import ykismobkmp.composeapp.generated.resources.edit
import ykismobkmp.composeapp.generated.resources.forward
import ykismobkmp.composeapp.generated.resources.message_actions_title


sealed class ChatItem {
  data class DateHeader(val date: String) : ChatItem()
  data class MessageItem(val message: MessageEntity) : ChatItem()
}

/**
 * [ChatScreenStateful] — Кроссплатформенный Stateful-контейнер экрана сообщений ЮКІС чата.
 * ПОЯСНЕНИЕ: Вычисляет UID комнаты чата в зависимости от роли (жилец/админ) и перенаправляет кадр.
 */
private const val className = "ChatScreen"
class ChatScreen(
  private val onBackClick: () -> Unit = {} // Передаем сквозной коллбек возврата Хаба смартфона
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val adaptiveNavigationType = LocalContentType.current

    // Кроссплатформенная инжекция Koin ScreenModel финансового и чат хаба ЮКІС
    val chatScreenModel = koinInject<ChatScreenModel>()
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()

    // Реактивно вычитываем живой стейт БТИ квартиры Южного
    val baseUIState by apartmentScreenModel.uiState.collectAsState()
    val selectedUser by chatScreenModel.selectedUser.collectAsState()

    // ИСПРАВЛЕНО НАМЕРТВО: Принудительная синхронизация глобальной квартиры при входе в чат.
    // Если житель заходит в чат квартиры №X, всё приложение (БТИ, Финансы, Счетчики) 
    // мгновенно переключается на эту квартиру №X.
    LaunchedEffect(selectedUser.addressId) {
      val isResident = baseUIState.userRole == UserRole.StandardUser
      if (isResident && selectedUser.addressId != 0L && selectedUser.addressId != baseUIState.addressId) {
        println("[$className.Sync]: Глобальне перемикання на квартиру о/р ${selectedUser.addressId} при вході в чат.")
        apartmentScreenModel.setAddressId(selectedUser.addressId)
      }
    }

    // Вызываем Stateful-компоновщик, связывая все слои в едином инстансе ОЗУ
    ChatScreenStateful(
      screenModel = chatScreenModel,
      baseUIState = baseUIState,
      navigationType = NavigationType.BOTTOM_NAVIGATION,
      onBackClick = {
        println("[$className.onBackClick]: Натиснуто стрілку назад чату. Повернення через Voyager.")
        onBackClick()
        navigator.pop() // Нативно сбрасываем кадр из стека навигации
      }
    )
  }
}

/**
 * [ChatScreenStateful] — Кроссплатформенный Stateless-компоновщик логики комнат чата.
 */
@Composable
fun ChatScreenStateful(
  screenModel: ChatScreenModel,
  baseUIState: BaseUIState,
  navigationType: NavigationType,
  onBackClick: () -> Unit // Колбек для безопасного возврата в String-роутер Хаба смартфона
) {
  val navigator = LocalNavigator.currentOrThrow
  val selectedUser by screenModel.selectedUser.collectAsState()

  val chatUid = remember(baseUIState.userRole, selectedUser, baseUIState.uid) {
    val uid = if (baseUIState.userRole == UserRole.StandardUser) {
      baseUIState.uid?.toString() ?: ""
    } else {
      selectedUser.uid ?: ""
    }
    println("[YkisLogKMP.$className.ChatScreenStateful]: [RE-CALC_ROOM] Префікс UID сесії: ${uid.takeLast(5)}")
    uid
  }

  ChatScreenContent(
    userEntity = selectedUser,
    screenModel = screenModel,
    baseUIState = baseUIState,
    navigationType = navigationType,
    chatUid = chatUid,
    navigateBack = onBackClick, // Плавный возврат на родительский список чатов без падения Voyager
    navigateToSendImageScreen = {
      // Передаем живой путь String и адрес БТИ квартиры в конструктор экрана предварительного просмотра
      val path = screenModel.selectedImagePath.value ?: ""
      val addressText = baseUIState.address ?: "м. Южне"
      println("[YkisLogKMP.$className.ChatScreenStateful.onImageSent]: Перехід до екрану надсилання медіа. Шлях: $path")
      navigator.push(SendImageScreen(imagePath = path, address = addressText))
    },
    navigateToCameraScreen = {
      // КМР Навигация Voyager для перехода в интерфейс встроенной камери смартфона
      println("[YkisLogKMP.$className.ChatScreenStateful.onCameraClick]: Запуск апаратної камери...")
      navigator.push(CameraScreenDest)
    },
    navigateToImageDetailScreen = { message ->
      // Передаем сквозную стейт-модель в ImageDetailScreen, полностью ликвидруя утечки памяти в бэкстеке iOS/Mac
      println("[YkisLogKMP.$className.ChatScreenStateful.onImageDetailClick]: Повноекранний перегляд фото для репліки з ID: ${message.id}")
      screenModel.setSelectedMessage(message)
      navigator.push(ImageDetailScreen(screenModel = screenModel))
    }
  )
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreenContent(
  modifier: Modifier = Modifier,
  userEntity: UserEntity,
  screenModel: ChatScreenModel,
  baseUIState: BaseUIState,
  navigationType: NavigationType,
  navigateBack: () -> Unit,
  navigateToSendImageScreen: () -> Unit,
  chatUid: String,
  navigateToCameraScreen: () -> Unit,
  navigateToImageDetailScreen: (MessageEntity) -> Unit
) {
  val messageText by screenModel.messageText.collectAsState()
  val messageList by screenModel.firebaseTest.collectAsState()
  val selectedService by screenModel.selectedService.collectAsState()
  val isLoadingAfterSending by screenModel.isLoadingAfterSending.collectAsState()
  val aiAssistantResponse by screenModel.assistantResponse.collectAsState()
  val isOpponentTyping by screenModel.isOpponentTyping.collectAsState()

  // Исправлено: Ссылка на несуществующий uiState удалена, подписка идет напрямую в запечатанный поток!
  val isForwardingMode by screenModel.isForwardingMode.collectAsState()

  val editingMessage by screenModel.editingMessage.collectAsState()

  val selectedServicePrefix by screenModel.selectedServicePrefix.collectAsState()

  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val listState = rememberLazyListState()
  val screenScope = rememberCoroutineScope()
  val myUid = baseUIState.uid.toString()

  // АВТОМАТИЗАЦИЯ ВЫБОРА ФАЙЛОВ: Инициализируем платформенный пикер
  val filePicker = rememberFilePicker()

  // Исправлено: Контур вычисления кодов ЖЕК/ОСББ переведен на сквозной Long-стандарт СУБД ЮКІС
  val currentChatOsbbId = remember(baseUIState.userRole, baseUIState.osbbId) {
    when (baseUIState.userRole) {
      UserRole.YtkeUser -> 9998L
      UserRole.VodokanalUser -> 9999L
      UserRole.TboUser -> 9997L
      UserRole.OsbbUser -> baseUIState.osbbId ?: 0L
      else -> baseUIState.osmdId ?: 0L
    }
  }

  // Упаковка и группировка сообщений по датам. Вызовы android.util.Log полностью удалены.
  val chatItems = remember<List<ChatItem>>(messageList, myUid) {
    println("[YkisLogKMP.$className.Filter]: Обробка стрічки повідомлень. Кількість: ${messageList.size} шт.")
    val filtered = messageList.filter { msg ->
      val deletedList = msg.deletedFor ?: emptyList()
      !deletedList.contains(myUid)
    }
    filtered.groupBy { com.ykis.ykismobkmp.core.utils.formatDateFull(it.timestamp) }
      .flatMap { (date, messages) ->
        listOf(ChatItem.DateHeader(date)) + messages.map { ChatItem.MessageItem(it) }
      }
  }

  // ИСПРАВЛЕНО: Автоматический доскролл вниз при открытии клавиатуры (IME)
  val imeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
  LaunchedEffect(imeHeight) {
    if (imeHeight > 0.dp && chatItems.isNotEmpty()) {
      println("[YkisLogKMP.$className.Scroll]: Клавіатура відкрита, виконуємо доскрол.")
      listState.animateScrollToItem(chatItems.size - 1)
    }
  }

  // Атомарный триггер подписки на Firebase-ветку сокетов при инициализации кадра чата смартфона
  LaunchedEffect(baseUIState.addressId, baseUIState.userRole, chatUid, userEntity.uid) {
    val role = baseUIState.userRole
    val addrId = if (role == UserRole.StandardUser) baseUIState.addressId else userEntity.addressId
    val targetUid = if (role == UserRole.StandardUser) chatUid else userEntity.uid ?: ""

    if (role != UserRole.Unknown && addrId > 0L && targetUid.isNotBlank()) {
      screenModel.readFromDatabase(
        role = role,
        senderUid = targetUid,
        osbbId = currentChatOsbbId,
        addressId = addrId
      )
    }
  }

  // Гарантированная зачистка индикаторов набора текста при закрытии или уничтожении экрана чата
  DisposableEffect(Unit) {
    onDispose {
      println("[YkisLogKMP.$className.onDispose]: Очищение контекста чата и сброс флагов ввода.")
      screenModel.clearCurrentChatPath() // ИСПРАВЛЕНО: Вызываем полную очистку контекста (Job + Path)
      screenModel.cancelForwarding()
    }
  }

  // Автоматический плавный доскролл LazyColumn вниз при прилете нового сообщения ГИОЦ/ОСББ
  LaunchedEffect(chatItems.size) {
    if (chatItems.isNotEmpty()) {
      listState.animateScrollToItem(chatItems.size - 1)
    }
  }

  val appBarTitle = remember<String>(
    baseUIState.osbb,
    selectedService,
    selectedServicePrefix,
    isForwardingMode,
    baseUIState.userRole,
    userEntity
  ) {
    when {
      isForwardingMode -> "Переслати повідомлення"
      baseUIState.userRole == UserRole.StandardUser -> {
        // ИСПРАВЛЕНО: Если объект службы пуст (переход по пушу), вычисляем имя по префиксу
        val serviceName = selectedService?.name 
          ?: when(selectedServicePrefix) {
            "WATER_SERVICE"   -> "КП \"ЮЖВОДОКАНАЛ\""
            "WARM_SERVICE"    -> "КП тм \"ЮТКЕ\""
            "GARBAGE_SERVICE" -> "КП \"СПЕЦТРАНС\""
            "OSBB"            -> baseUIState.osbb.takeIf { !it.isNullOrBlank() && it != "0" } ?: "ОСББ"
            else              -> ""
          }

        if (selectedService?.contentDetail == ContentDetail.OSBB || selectedServicePrefix == "OSBB") {
          baseUIState.osbb.takeIf { !it.isNullOrBlank() && it != "0" } ?: "ОСББ"
        } else {
          serviceName
        }
      }

      else -> {
        userEntity.displayName?.substringBefore("|")?.trim() ?: "Чат диспетчера"
      }
    }
  }

  val appBarSubtitle =
    remember<String>(baseUIState.address, userEntity, isOpponentTyping, baseUIState.userRole) {
      when {
        isOpponentTyping -> "друкує..."
        baseUIState.userRole == UserRole.StandardUser -> baseUIState.address ?: ""
        else -> {
          // Исправлено: Отображение о/р приведено к безопасному Long без ToInt() урезания!
          val accountNum = userEntity.addressId
          if (accountNum != 0L) {
            "о/р: $accountNum"
          } else {
            userEntity.displayName?.substringAfter("|")?.trim() ?: ""
          }
        }
      }
    }


  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    contentWindowInsets = WindowInsets(0, 0, 0, 0), // ИСПРАВЛЕНО: Отключаем дублирование системных отступов
    topBar = {
      Column {
        DefaultAppBar(
          title = appBarTitle,
          subtitle = appBarSubtitle,
          canNavigateBack = true,
          onBackClick = {
            keyboardController?.hide()
            focusManager.clearFocus()
            if (isForwardingMode) screenModel.cancelForwarding() else navigateBack()
          },
          navigationType = navigationType
        )
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
      }
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .imePadding() // ГАРАНТИЯ: Поле ввода будет подниматься над клавиатурой
        ) {
          AnimatedVisibility(visible = !aiAssistantResponse.isNullOrBlank()) {
            AiHintCard(
              text = aiAssistantResponse ?: "",
              title = if (baseUIState.userRole != UserRole.StandardUser) "Порада диспетчеру" else "Помічник",
              onClose = { screenModel.clearAiSuggestion() },
              onApply = {
                println("[YkisLogKMP.$className.AiApply]: Інтеграція тексту підказки Gemini в поле введення.")
                screenModel.applyAiHint()
              }
            )
          }
          ComposeMessageBox(
            text = messageText,
            onTextChanged = { screenModel.onMessageTextChanged(it) },
            onSent = {
              if (editingMessage != null) {
                println("[YkisLogKMP.$className.onSent]: Оновлення існуючого повідомлення в Firebase.")
                screenModel.updateMessage(messageText)
              } else {
                val isResident = baseUIState.userRole == UserRole.StandardUser
                val curAddrId = userEntity.addressId

                // ИСПРАВЛЕНО НАМЕРТВО: Прямой поиск анкеты по ID чата.
                // Это гарантирует, что сообщение будет подписано фамилией именно ТОЙ квартиры,
                // даже если глобальный стейт еще не успел переключиться.
                val chatApt = if (isResident) {
                  baseUIState.apartments.find { it.addressId == curAddrId }
                } else null

                val curAddr = if (isResident) {
                  chatApt?.address ?: baseUIState.address ?: ""
                } else {
                  userEntity.displayName?.substringBefore("|")?.trim() ?: ""
                }

                val senderName = if (isResident) {
                  val surname = chatApt?.nanim ?: baseUIState.nanim ?: "Мешканець"
                  "$curAddr | $surname"
                } else {
                  baseUIState.displayName ?: "Адміністратор"
                }

                println("[YkisLogKMP.$className.onSent]: Відправка повідомлення. Адреса: $curAddr, Абонент: $senderName")

                screenModel.writeToDatabase(
                  chatUid = chatUid,
                  senderUid = myUid,
                  senderDisplayedName = senderName,
                  senderLogoUrl = baseUIState.photoUrl,
                  senderAddress = curAddr,
                  addressId = curAddrId,
                  imageUrl = null,
                  fileUrl = null,
                  fileName = null,
                  osbbId = currentChatOsbbId,
                  role = baseUIState.userRole,
                  recipientTokens = userEntity.tokens ?: emptyList(),
                  onComplete = {
                    println("[YkisLogKMP.$className.onSent]: Транзакція успішна, очищення ШІ контексту.")
                    screenModel.clearAiSuggestion()
                  }
                )
              }
            },
            onImageSent = { path ->
              // ИСПРАВЛЕНО: Если путь пустой, значит нажали на иконку вложений — открываем пикер
              if (path.isBlank()) {
                println("[YkisLogKMP.$className.onAttach]: Открытие системного окна выбора документов.")
                filePicker.pickFile { pickedPath ->
                  println("[YkisLogKMP.$className.onAttach]: Файл выбран: $pickedPath")
                  screenModel.setSelectedImagePath(pickedPath)
                  navigateToSendImageScreen()
                }
              } else {
                println("[YkisLogKMP.$className.onImageSent]: Добавление медиафайла за путем: $path")
                screenModel.setSelectedImagePath(path)
                if (baseUIState.userRole == UserRole.StandardUser) {
                  println("[YkisLogKMP.$className.Gemini]: Автоматический запуск компьютерного зрения Gemini.")
                  screenModel.analyzePhotoWithGemini(path, baseUIState.address)
                }
                navigateToSendImageScreen()
              }
            },
            onAiClick = {
              if (messageText.isNotBlank()) {
                println("[YkisLogKMP.$className.onAiClick]: Запит генерації тексту у нейромережі Gemini.")
                // Исправлено: Параметры приведены к 100% запечатанной сигнатуре вьюмодели чата!
                screenModel.askAssistant(
                  prompt = messageText,
                  currentRole = baseUIState.userRole,
                  currentAddress = baseUIState.address ?: "м. Южне"
                )
              }
            },
            onCameraClick = {
              println("[YkisLogKMP.$className.onCamera]: Запуск апаратної камери смартфона.")
              navigateToCameraScreen()
            },
            isLoading = isLoadingAfterSending,
            canSend = messageText.isNotBlank() || editingMessage != null
          )
        }
      }
    }
  ) { innerPadding ->
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    // Подписываемся на стейт выбора сообщения для действий
    val messageToDelete by screenModel.messageToDelete.collectAsState()

    // ВЫЗОВ КОНТЕКСТНОГО МЕНЮ (Удаление, Редактирование, Пересылка)
    if (messageToDelete != null) {
      MessageActionsDialog(
        messageToDelete = messageToDelete,
        myUid = myUid,
        screenModel = screenModel,
        navigateBack = {
          // Если мы в режиме пересылки, нужно вернуться к списку чатов
          if (screenModel.forwardingMessage.value != null) {
            navigateBack()
          }
        }
      )
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = innerPadding.calculateTopPadding()),
      state = listState,
      contentPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 8.dp,
        // ИСПРАВЛЕНО: Увеличиваем нижний паддинг, чтобы сообщения не "залипали" под полем ввода
        bottom = innerPadding.calculateBottomPadding() + 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      chatItems.forEach { chatItem ->
        when (chatItem) {
          is ChatItem.DateHeader -> {
            stickyHeader(key = "date_${chatItem.date}") {
              DateChip(date = chatItem.date)
            }
          }

          is ChatItem.MessageItem -> {
            val msg = chatItem.message
            val uniqueKey = "msg_${msg.id}_${msg.timestamp}"
            item(key = uniqueKey) {
              MessageListItem(
                uid = myUid,
                isUserAdmin = baseUIState.userRole != UserRole.StandardUser,
                messageEntity = msg,
                onLongClick = {
                  println("[YkisLogKMP.$className.onLongClick]: Активація контекстного меню для ID: ${msg.id}")
                  screenModel.showDeleteConfirmation(msg)
                },
                onClick = {
                  keyboardController?.hide()
                  navigateToImageDetailScreen(msg)
                },
                onFileClick = { fileUrl ->
                  try {
                    uriHandler.openUri(fileUrl)
                  } catch (e: Exception) {
                    println("[YkisLogKMP.$className.UriError_ERR]: Не вдалося відкрити документ: ${e.message}")
                  }
                }
              )
            }
          }
        }
      }
    }
  }
}
  @Composable
  fun MessageActionsDialog(
    messageToDelete: MessageEntity?,
    myUid: String,
    screenModel: ChatScreenModel,
    navigateBack: () -> Unit
  ) {
    if (messageToDelete == null) return
    val isMyMessage = messageToDelete.senderUid == myUid

    AlertDialog(
      onDismissRequest = { screenModel.dismissDeleteDialog() },
      title = {
        Text(
          text = stringResource(Res.string.message_actions_title),
          style = MaterialTheme.typography.titleMedium
        )
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // 1. РЕДАКТИРОВАНИЕ (Доступно только для собственных текстовых реплик)
          if (isMyMessage && messageToDelete.imageUrl == null) {
            TextButton(
              onClick = {
                println("[YkisLogKMP.ChatScreen.Edit]: Редагування повідомлення з ID: ${messageToDelete.id}")
                screenModel.startEditing(messageToDelete)
                screenModel.dismissDeleteDialog()
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Edit, contentDescription = null)
              Spacer(Modifier.width(12.dp))
              Text(stringResource(Res.string.edit), modifier = Modifier.weight(1f))
            }
          }

          // 2. ПЕРЕСЫЛКА (Доступна для любого входящего или исходящего сообщения)
          TextButton(
            onClick = {
              println("[YkisLogKMP.ChatScreen.Forward]: Ініціалізація пересилання для ID: ${messageToDelete.id}")
              screenModel.startForwarding(messageToDelete)
              screenModel.dismissDeleteDialog()
              navigateBack()
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Reply,
              modifier = Modifier.graphicsLayer(scaleX = -1f),
              contentDescription = null
            )
            Spacer(Modifier.width(12.dp))
            Text(stringResource(Res.string.forward), modifier = Modifier.weight(1f))
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

          // 3. УДАЛИТЬ У МЕНЯ (Локальное сокрытие строки из персональной ленты смартфона)
          TextButton(
            onClick = {
              println("[YkisLogKMP.ChatScreen.DeleteLocal]: Локальне приховування повідомлення з ID: ${messageToDelete.id}")
              screenModel.deleteForMe(messageToDelete.id)
              screenModel.dismissDeleteDialog()
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(Res.string.delete_for_me), modifier = Modifier.weight(1f))
          }

          // 4. УДАЛИТЬ У ВСЕХ (Безвозвратное удаление строки из облачной базы Firebase)
          if (isMyMessage) {
            TextButton(
              onClick = {
                println("[YkisLogKMP.ChatScreen.DeleteGlobal]: Глобальне видалення повідомлення з ID: ${messageToDelete.id}")
                screenModel.confirmDeletion()
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
              )
              Spacer(Modifier.width(12.dp))
              Text(
                text = stringResource(Res.string.delete_for_everyone),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { screenModel.dismissDeleteDialog() }) {
          Text(stringResource(Res.string.cancel))
        }
      }
    )
  }

  @Composable
  fun AiHintCard(
    text: String,
    title: String,
    onClose: () -> Unit,
    onApply: () -> Unit
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
      color = MaterialTheme.colorScheme.secondaryContainer,
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 4.dp,
      shadowElevation = 2.dp
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "AI Hint",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Spacer(Modifier.weight(1f))
          IconButton(
            onClick = onClose,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              modifier = Modifier.graphicsLayer(scaleX = 0.7f, scaleY = 0.7f)
            )
          }
        }
        Text(
          text = text,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(vertical = 4.dp)
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
          Text(
            text = stringResource(Res.string.apply_suggestion),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .clickable {
                println("[YkisLogKMP.AiHintCard.onApply]: Застосування інтелектуальної підказки Gemini.")
                onApply()
              }
              .padding(top = 4.dp)
          )
        }
      }
    }
  }

  @Composable
  fun DateChip(date: String) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text(
          text = date,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer
        )
      }
    }
  }





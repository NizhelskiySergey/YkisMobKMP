package com.ykis.ykismobkmp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.ui.navigation.YkisPamApp
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreenModel
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatform
@Composable
fun YkisPamAppRoot(
  windowSize: WindowSizeClass,
  displayFeatures: List<Any>,
  initialChatId: String?
) {
  var isKoinReady by remember {
    mutableStateOf(
      try {
        KoinPlatform.getKoin() != null
      } catch (e: Exception) {
        false
      }
    )
  }

  if (!isKoinReady) {
    LaunchedEffect(Unit) {
      while (true) {
        val ready = try { KoinPlatform.getKoin() != null } catch (e: Exception) { false }
        if (ready) {
          isKoinReady = true
          break
        }
        delay(10L)
      }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
  } else {
    val koin = KoinPlatform.getKoin()
    val dbQueries = remember { koin.get<YkisDatabasesQueries>() }

    // Безопасно будим SQLite-драйвер Cash App и подтягиваем конфигурацию оферты из облака
    LaunchedEffect(Unit) {
      try {
        // 1. Асинхронно скачиваем свежие параметры оферты из Firebase Remote Config
        val firebaseService = koin.get<FirebaseService>()
        firebaseService.fetchConfiguration()

        // 2. Опрашиваем локальный кэш квартир
        val cachedFlatsCount = dbQueries.getApartmentList().executeAsList().size
        println("[YkisLogKMP.App.YkisPamAppRoot]: СУБД и Remote Config опрошены. Квартир в кэше: $cachedFlatsCount")
      } catch (e: Exception) {
        println("[YkisLogKMP.App.YkisPamAppRoot_CRITICAL_FAIL]: Сбой инициализации СУБД или Firebase на старте: ${e.message}")
        e.printStackTrace()
      }
    }

    // Запускаем инжекцию темы оформления (Твой оригинальный чистый вызов)
    // Из-за того, что в Koin мы прописали single, этот инстанс жестко синхронизирован с экраном настроек!
    val settingsScreenModel = koinInject<SettingsScreenModel>()
    val currentTheme by settingsScreenModel.theme.collectAsState()

    println("[YkisLogKMP.App.YkisPamAppRoot]: Граф DI верифіковано. Ініціалізація теми ЮКІС: ${currentTheme ?: "system"}")

    YkisPAMTheme(appTheme = currentTheme ?: "system") {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
      ) {
        YkisPamApp(
          windowSize = windowSize,
          displayFeatures = displayFeatures,
          initialChatId = initialChatId
        )
      }
    }
  }
}

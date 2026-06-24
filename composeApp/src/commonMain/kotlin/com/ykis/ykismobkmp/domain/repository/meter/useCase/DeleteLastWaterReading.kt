package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jetbrains.compose.resources.getString
import ykismobkmp.composeapp.generated.resources.*

/**
 * [DeleteLastWaterReading] — Use Case видалення показань води.
 * УНІФІКОВАНО: Локалізація та стандартне логування.
 */
class DeleteLastWaterReading(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "DeleteLastWaterReading"

  operator fun invoke(uid: String, readingId: Long): Flow<Resource<GetSimpleResponse?>> = flow {
    try {
      emit(Resource.Loading())

      val response = repository.deleteLastWaterReading(uid, readingId)

      if (response.success == 1) {
        // На вебі ми ігноруємо кеш
        if (!com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
            try { meterCache.deleteWaterReadingByPokId(readingId) } catch (e: Exception) { }
        }
        
        emit(Resource.Success<GetSimpleResponse?>(response))
        SnackbarManager.showMessage(getString(Res.string.success_delete))
      } else {
        val errorMsg = response.message ?: getString(Res.string.error_server)
        emit(Resource.Error<GetSimpleResponse?>(message = errorMsg))
        SnackbarManager.showMessage(errorMsg)
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.$className]: [ERROR] ${ex.message}")
      val netError = getString(Res.string.error_network_request_failed)
      emit(Resource.Error<GetSimpleResponse?>(message = netError))
      SnackbarManager.showMessage(netError)
    }
  }.flowOn(Dispatchers.Default)
}

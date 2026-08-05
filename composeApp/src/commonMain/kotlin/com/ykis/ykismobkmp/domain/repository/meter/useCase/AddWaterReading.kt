package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jetbrains.compose.resources.getString
import com.ykis.ykismobkmp.*

/**
 * [AddWaterReading] — Use Case передачі показань води.
 * УНІФІКОВАНО: Локалізовані повідомлення та стандартне логування.
 */
class AddWaterReading(
  private val repository: MeterRepository
) {
  private val className = "AddWaterReading"

  operator fun invoke(
    uid: String,
    vodomerId: Long,
    currentValue: Long,
    newValue: Long
  ): Flow<Resource<GetSimpleResponse?>> = flow {
    try {
      emit(Resource.Loading())

      val response = repository.addWaterReading(
        uid = uid,
        vodomerId = vodomerId,
        currentValue = currentValue,
        newValue = newValue
      )

      if (response.success == 1) {
        emit(Resource.Success<GetSimpleResponse?>(response))
        SnackbarManager.showMessage(getString(Res.string.reading_added))
      } else {
        val errorMsg = response.message
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

package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jetbrains.compose.resources.getString
import ykismobkmp.composeapp.generated.resources.*

/**
 * [GetWaterMeterList] — Use Case для отримання списку водомірів.
 * УНІФІКОВАНО: На Вебі працюємо виключно через мережу, на мобілках — з кешуванням.
 */
class GetWaterMeterList(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetWaterMeterList"

  operator fun invoke(uid: String, addressId: Long): Flow<Resource<List<WaterMeterEntity>>> = flow {
    if (uid.isBlank() || addressId <= 0L) {
      emit(Resource.Success(emptyList()))
      return@flow
    }

    emit(Resource.Loading())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) || 
                com.ykis.ykismobkmp.getPlatform().name.contains("JS", true)

    try {
      // 1. СПРОБА КЕШУ (тільки для мобільних)
      if (!isWeb) {
          try {
              val local = meterCache.getWaterMetersByApartment(addressId)
              if (local.isNotEmpty()) emit(Resource.Success(local))
          } catch (e: Exception) { }
      }

      // 2. МЕРЕЖЕВИЙ ЗАПИТ
      val response = repository.getWaterMeterList(uid, addressId)
      val remoteMeters = response.waterMeters

      if (response.success == 1) {
        emit(Resource.Success(remoteMeters))

        // 3. ОНОВЛЕННЯ КЕШУ (тільки для мобільних)
        if (!isWeb && remoteMeters.isNotEmpty()) {
            try {
                meterCache.deleteWaterMetersByApartment(addressId)
                meterCache.insertWaterMeter(remoteMeters)
            } catch (e: Exception) { }
        }
      } else {
          // Якщо сервер не повернув дані - гасимо лоадер порожнім списком
          emit(Resource.Success(emptyList()))
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.${className}_ERROR]: ${ex.message}")
      emit(Resource.Success(emptyList()))
    }
  }.flowOn(Dispatchers.Default)
}

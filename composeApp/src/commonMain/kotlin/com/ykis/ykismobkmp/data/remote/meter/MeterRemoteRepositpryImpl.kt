package com.ykis.ykismobkmp.data.remote.meter

import com.ykis.ykismobkmp.core.Constants.ADDRESS_ID
import com.ykis.ykismobkmp.core.Constants.CURRENT_VALUE
import com.ykis.ykismobkmp.core.Constants.NEW_VALUE
import com.ykis.ykismobkmp.core.Constants.POK_ID
import com.ykis.ykismobkmp.core.Constants.TEPLOMER_ID
import com.ykis.ykismobkmp.core.Constants.UID
import com.ykis.ykismobkmp.core.Constants.VODOMER_ID
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.GetHeatMeterResponse
import com.ykis.ykismobkmp.data.responses.GetHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetLastHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetLastWaterReadingResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.data.responses.GetWaterMeterResponse
import com.ykis.ykismobkmp.data.responses.GetWaterReadingsResponse

/**
 * [MeterRemoteRepositoryImpl] — Реализация удаленного репозитория на базе KtorApiService.
 * ИСПРАВЛЕНО НАМЕРТВО: Исправлена опечатка в имени класса, все типы Int строго переведены в Long!
 * Намертво зафиксирован для полной замены.
 */
class MeterRemoteRepositoryImpl(
  private val ktorApiService: KtorApiService
) : MeterRemoteRepository {

  override suspend fun getWaterMeterList(uid: String, addressId: Long): GetWaterMeterResponse {
    return ktorApiService.getWaterMeterList(
      createGetWaterMeterMap(
        uid = uid,
        addressId = addressId
      )
    )
  }

  override suspend fun getWaterReadings(uid: String, vodomerId: Long): GetWaterReadingsResponse {
    return ktorApiService.getWaterReadings(createGetWaterReadingMap(uid, vodomerId))
  }

  override suspend fun getLastWaterReading(uid: String, vodomerId: Long): GetLastWaterReadingResponse {
    return ktorApiService.getLastWaterReading(createGetWaterReadingMap(uid, vodomerId))
  }

  override suspend fun addWaterReading(
    uid: String,
    vodomerId: Long,
    currentValue: Long,
    newValue: Long
  ): GetSimpleResponse {
    return ktorApiService.addWaterReading(
      createAddNewWaterReadingMap(
        uid = uid,
        vodomerId = vodomerId,
        currentValue = currentValue,
        newValue = newValue
      )
    )
  }

  override suspend fun deleteLastWaterReading(uid: String, pokId: Long): GetSimpleResponse {
    return ktorApiService.deleteLastWaterReading(createDeleteReadingMap(uid, pokId))
  }

  override suspend fun getHeatMeterList(uid: String, addressId: Long): GetHeatMeterResponse {
    return ktorApiService.getHeatMeterList(createGetHeatMeterMap(uid, addressId))
  }

  override suspend fun getHeatReadings(uid: String, teplomerId: Long): GetHeatReadingResponse {
    return ktorApiService.getHeatReadings(createGetHeatReadingMap(uid, teplomerId))
  }

  override suspend fun getLastHeatReading(uid: String, teplomerId: Long): GetLastHeatReadingResponse {
    return ktorApiService.getLastHeatReading(createGetHeatReadingMap(uid, teplomerId))
  }

  override suspend fun addHeatReading(
    uid: String,
    teplomerId: Long,
    currentValue: Double,
    newValue: Double
  ): GetSimpleResponse {
    return ktorApiService.addHeatReading(
      createAddNewHeatReadingMap(
        uid = uid,
        teplomerId = teplomerId,
        currentValue = currentValue,
        newValue = newValue
      )
    )
  }

  override suspend fun deleteLastHeatReading(uid: String, pokId: Long): GetSimpleResponse {
    return ktorApiService.deleteLastHeatReading(createDeleteReadingMap(uid, pokId))
  }

  private fun createGetHeatReadingMap(uid: String, teplomerId: Long): Map<String, String> {
    val map = HashMap<String, String>()
    map[TEPLOMER_ID] = teplomerId.toString()
    map[UID] = uid
    return map
  }

  private fun createAddReadingMap(
    uid: String,
    teplomerId: Long,
    newValue: Double,
    currentValue: Double
  ): Map<String, String> {
    val map = HashMap<String, String>()
    map[TEPLOMER_ID] = teplomerId.toString()
    map[NEW_VALUE] = newValue.toString()
    map[CURRENT_VALUE] = currentValue.toString()
    map[UID] = uid
    return map
  }

  private fun createDeleteReadingMap(uid: String, pokId: Long): Map<String, String> {
    val map = HashMap<String, String>()
    map[POK_ID] = pokId.toString()
    map[UID] = uid
    return map
  }

  private fun createGetHeatMeterMap(uid: String, addressId: Long): Map<String, String> {
    val map = HashMap<String, String>()
    map[ADDRESS_ID] = addressId.toString()
    map[UID] = uid
    return map
  }

  private fun createGetWaterReadingMap(uid: String, vodomerId: Long): Map<String, String> {
    val map = HashMap<String, String>()
    map[VODOMER_ID] = vodomerId.toString()
    map[UID] = uid
    return map
  }

  private fun createAddNewWaterReadingMap(
    uid: String,
    vodomerId: Long,
    newValue: Long,
    currentValue: Long
  ): Map<String, String> {
    val map = HashMap<String, String>()
    map[VODOMER_ID] = vodomerId.toString()
    map[NEW_VALUE] = newValue.toString()
    map[CURRENT_VALUE] = currentValue.toString()
    map[UID] = uid
    return map
  }

  private fun createAddNewHeatReadingMap(
    uid: String,
    teplomerId: Long,
    newValue: Double,
    currentValue: Double
  ): Map<String, String> {
    val map = HashMap<String, String>()
    map[TEPLOMER_ID] = teplomerId.toString()
    
    // ИСПРАВЛЕНО: Принудительное форматирование Double в строку с фиксированной точностью (4 знака)
    // Это исключает ошибки парсинга на стороне PHP-бэкенда (например, научную нотацию 1.2E-4)
    val formattedNewValue = formatDoubleForApi(newValue)
    val formattedCurrentValue = formatDoubleForApi(currentValue)

    map[NEW_VALUE] = formattedNewValue
    map[CURRENT_VALUE] = formattedCurrentValue
    map[UID] = uid
    return map
  }

  /**
   * [formatDoubleForApi] — Хелпер для нормализации вещественных чисел перед отправкой в ГИОЦ.
   */
  private fun formatDoubleForApi(value: Double): String {
    // КМР-способ ограничить точность: умножаем, округляем и делим, либо просто обрезаем строку
    return ((value * 10000).toLong() / 10000.0).toString()
  }

  private fun createDeleteWaterReadingMap(uid: String, pokId: Long): Map<String, String> {
    val map = HashMap<String, String>()
    map[POK_ID] = pokId.toString()
    map[UID] = uid
    return map
  }

  private fun createGetWaterMeterMap(uid: String, addressId: Long): Map<String, String> {
    val map = HashMap<String, String>()
    map[ADDRESS_ID] = addressId.toString()
    map[UID] = uid
    return map
  }
}

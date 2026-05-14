package com.ykis.ykismobkmp.domain.repository.meter // Твой сетевой пакет данных счетчиков воды

import com.ykis.ykismobkmp.data.responses.GetLastWaterReadingResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.data.responses.GetWaterMeterResponse
import com.ykis.ykismobkmp.data.responses.GetWaterReadingsResponse

/**
 * [com.ykis.ykismobkmp.data.water.WaterMeterRemoteRepository] — Контракт сетевого слоя (Ktor DataSource) для счетчиков воды.
 * Все числовые параметры переведены на тип Long под стандарты SQLDelight.
 */
data class AddWaterReadingParams(
  val uid : String,
  val meterId: Int,
  val newValue: Int,
  val currentValue: Int
)
interface WaterMeterRepository { // Или interface WaterMeterRemoteRepository, проверь точное имя

  // ИСПРАВЛЕНО: Типы Int заменены на Long во всех сигнатурах удаленных запросов
  suspend fun getWaterMeterList(uid: String, addressId: Long): GetWaterMeterResponse

  suspend fun getWaterReadings(uid: String, vodomerId: Long): GetWaterReadingsResponse

  suspend fun getLastWaterReading(uid: String, vodomerId: Long): GetLastWaterReadingResponse

  // ИСПРАВЛЕНО: Принимает сквозную доменную структуру параметров
  suspend fun addWaterReading(params: AddWaterReadingParams): GetSimpleResponse

  suspend fun deleteLastReading(uid: String, readingId: Long): GetSimpleResponse
}

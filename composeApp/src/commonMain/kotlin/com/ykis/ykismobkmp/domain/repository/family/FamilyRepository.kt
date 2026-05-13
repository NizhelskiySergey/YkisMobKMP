package com.ykis.ykismobkmp.domain.repository.family


import com.ykis.ykismobkmp.data.responses.GetFamilyResponse
import com.ykis.ykismobkmp.domain.entity.FamilyEntity

/**
 * [FamilyRepository] — Доменный контракт для работы с составом семьи.
 * Все числовые идентификаторы переведены на KMP-совместимый тип Long
 * для бесшовной интеграции с Use Cases и локальной БД SQLDelight.
 */
interface FamilyRepository {

  /**
   * Получает список членов семьи по ID квартиры (addressId).
   * Используется как в обычном режиме жильца, так и в режиме админа/диспетчера на Mac.
   */
  suspend fun getFamilyList(addressId: Long): GetFamilyResponse

  /**
   * Очистка локальных данных (нужна для DeleteUserAccount или синхронизации).
   * Логика транзакций кэша пробрасывается через UseCase на уровень SQLDelight.
   */
  suspend fun clearLocalFamily()
}


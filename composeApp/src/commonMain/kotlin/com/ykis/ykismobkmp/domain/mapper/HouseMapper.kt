package com.ykis.ykismobkmp.domain.mapper


import com.ykis.ykismobkmp.db.HouseEntity as DbHouse
import com.ykis.ykismobkmp.domain.entity.HouseEntity as DomainHouse
/**
 * [DbHouse.toDomainHouse] — Преобразование сгенерированной сущности SQLDelight в доменную модель дома.
 * Безопасно приводит Long из базы данных к исходным Int типам бизнес-логики.
 */
fun DbHouse.toDomainHouse(): DomainHouse {
  return DomainHouse(
    // Конвертируем Long из БД обратно в Int доменной модели
    houseId = this.houseId,
    raionId = this.raionId,
    house = this.house      // String маппится напрямую символ в символ
  )
}

/**
 * [DomainHouse.toDbHouse] — Преобразование доменной модели дома в сущность SQLDelight для сохранения.
 * Конвертирует Int из домена в системный Long (INTEGER) для SQLite.
 */
fun DomainHouse.toDbHouse(): DbHouse {
  return DbHouse(
    // Приводим Int домена к Long для первичных и внешних ключей базы данных
    houseId = this.houseId.toLong(),
    raionId = this.raionId.toLong(),
    house = this.house
  )
}

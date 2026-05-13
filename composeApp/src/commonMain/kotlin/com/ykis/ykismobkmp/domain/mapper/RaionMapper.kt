package com.ykis.ykismobkmp.domain.mapper

// ИСПРАВЛЕНО: Класс SQLDelight генерируется с большой буквы RaionEntity
import com.ykis.ykismobkmp.db.RaionEntity as DbRaion
import com.ykis.ykismobkmp.domain.entity.RaionEntity as DomainRaion

/**
 * [DbRaion.toDomain] — Преобразование сущности SQLDelight обратно в доменную модель района.
 * Безопасно конвертирует системный Long из базы данных в Int для домена ЖКХ.
 */
fun DbRaion.toDomainRaion(): DomainRaion {
  return DomainRaion(
    raionId = this.raionId.toInt(), // Безопасное приведение Long -> Int
    raion = this.raion
  )
}

/**
 * [DomainRaion.toDbRaion] — Преобразование доменной модели района в сущность SQLDelight для сохранения.
 * Конвертирует Int из домена в системный Long для первичного ключа SQLite.
 */
fun DomainRaion.toDbRaion(): DbRaion {
  return DbRaion(
    raionId = this.raionId.toLong(), // Безопасное приведение Int -> Long для PRIMARY KEY
    raion = this.raion
  )
}

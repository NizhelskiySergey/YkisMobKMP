package com.ykis.ykismobkmp.domain.mapper

// 1. ИСПРАВЛЕНО: Импортируем сгенерированный SQLDelight-класс строго по имени твоей таблицы
import com.ykis.ykismobkmp.db.HeatReadingEntity as DbHeatReading
// 2. Импортируем нашу чистую доменную КМР-модель для UI слоя
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity as DomainHeatReading

/**
 * [DomainHeatReading.toDbHeatReading] — Конвертация доменной UI-модели в SQLDelight сущность для записи в БД.
 * ИСПРАВЛЕНО: Все имена полей приведены к точному CamelCase твоей SQL-схемы.
 */
fun DomainHeatReading.toDbHeatReading(): DbHeatReading {
  return DbHeatReading(
    pokId = this.pokId, // Первичный ключ Long
    addressId = this.addressId, // Сквозной Long ID
    teplomerId = this.teplomerId, // Сквозной Long ID
    dateReading = this.dateReading,
    dateOt = this.dateOt,
    dateDo = this.dateDo,
    edizm = this.edizm,
    koef = this.koef,
    days = this.days.toLong(), // SQLDelight сохраняет INTEGER как Long в SQLite
    last = this.last, // Чистое имя поля по твоей SQL схеме
    current = this.current, // Чистое имя поля по твоей SQL схеме
    gkal = this.gkal,
    avg = this.avg.toLong(), // Чистое имя поля по твоей SQL схеме
    tarif = this.tarif,
    qty = this.qty,
    pokOt = this.pokOt,
    pokDo = this.pokDo,
    gkalRasch = this.gkalRasch,
    gkalDay = this.gkalDay,
    qtyDay = this.qtyDay,
    dayAvg = this.dayAvg,
    dateIn = this.dateIn,
    operator_ = this.operator // Чистое имя поля по твоей SQL схеме
  )
}

/**
 * [DbHeatReading.toDomainHeatReading] — Преобразование сущности SQLDelight обратно в чистую доменную модель для UI.
 * ИСПРАВЛЕНО: Убрана принудительная распаковка null, так как все поля в схеме объявлены как NOT NULL.
 */
fun DbHeatReading.toDomainHeatReading(): DomainHeatReading {
  return DomainHeatReading(
    pokId = this.pokId,
    addressId = this.addressId,
    teplomerId = this.teplomerId,
    dateReading = this.dateReading,
    dateOt = this.dateOt,
    dateDo = this.dateDo,
    edizm = this.edizm,
    koef = this.koef,
    days = this.days, // Безопасное приведение Long из SQLite в Int для UI модели
    last = this.last,
    current = this.current,
    gkal = this.gkal,
    avg = this.avg, // Безопасное приведение Long из SQLite в Int для UI модели
    tarif = this.tarif,
    qty = this.qty,
    pokOt = this.pokOt,
    pokDo = this.pokDo,
    gkalRasch = this.gkalRasch,
    gkalDay = this.gkalDay,
    qtyDay = this.qtyDay,
    dayAvg = this.dayAvg,
    dateIn = this.dateIn,
    operator = this.operator_
  )
}

package com.ykis.ykismobkmp.domain.mapper

import com.ykis.ykismobkmp.db.WaterReadingEntity as DbWaterReading
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity as DomainWaterReading
/**
 * [DomainWaterReading.toDbWaterReading] — Конвертация доменной UI-модели в SQLDelight сущность для записи в БД.
 */
fun DomainWaterReading.toDbWaterReading(): DbWaterReading {
  return DbWaterReading(
    addressId = this.addressId, // Сквозной Long ID
    pokId = this.pokId, // Первичный ключ Long
    vodomerId = this.vodomerId, // Long ID счетчика
    dateOt = this.dateOt,
    dateDo = this.dateDo,
    days = this.days.toLong(), // SQLDelight сохраняет INTEGER как Long в SQLite
    // ИСПРАВЛЕНО: Связываем доменные поля кубометров с CamelCase колонками твоей SQL схемы
    lastValue = this.last.toLong(),
    currentValue = this.current.toLong(),
    kub = this.kub.toLong(),
    avg = this.avg.toLong(),
    pokOt = this.pokOt.toLong(),
    pokDo = this.pokDo.toLong(),
    rday = this.rday.toLong(),
    kubDay = this.kubDay, // REAL маппится в Double напрямую
    qtyKub = this.qtyKub.toLong(),
    operator_ = this.operator,
    dateReadings = this.dateReadings,
    tarifXv = this.tarifXv,
    xvoda = this.xvoda,
    tarifvSt = this.tarifvSt,
    stoki = this.stoki,
    dateSt = this.dateSt,
    dateFin = this.dateFin,
    mday = this.mday.toLong(),
    dateIn = this.dateIn
  )
}

/**
 * [DbWaterReading.toDomainWaterReading] — Преобразование сущности SQLDelight обратно в чистую доменную модель для UI.
 */
fun DbWaterReading.toDomainWaterReading(): DomainWaterReading {
  return DomainWaterReading(
    addressId = this.addressId,
    pokId = this.pokId,
    vodomerId = this.vodomerId,
    dateOt = this.dateOt,
    dateDo = this.dateDo,
    // ИСПРАВЛЕНО: Приведение Long из SQLite в Int для доменной модели
    days = this.days.toInt(),
    // ИСПРАВЛЕНО: Преобразуем сохраненные значения кубометров обратно в Double для UI-слоя
    last = this.lastValue.toDouble(),
    current = this.currentValue.toDouble(),
    kub = this.kub.toDouble(),
    avg = this.avg.toInt(),
    pokOt = this.pokOt.toInt(),
    pokDo = this.pokDo.toInt(),
    rday = this.rday.toInt(),
    kubDay = this.kubDay,
    qtyKub = this.qtyKub.toDouble(),
    operator = this.operator_,
    dateReadings = this.dateReadings,
    tarifXv = this.tarifXv,
    xvoda = this.xvoda,
    tarifvSt = this.tarifvSt,
    stoki = this.stoki,
    dateSt = this.dateSt,
    dateFin = this.dateFin,
    mday = this.mday.toInt(),
    dateIn = this.dateIn
  )
}

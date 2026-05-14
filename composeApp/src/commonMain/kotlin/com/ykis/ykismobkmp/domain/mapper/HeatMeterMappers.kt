package com.ykis.ykismobkmp.domain.mapper

// 1. Импортируем сгенерированный SQLDelight-класс строго по имени твоей таблицы CamelCase
import com.ykis.ykismobkmp.db.HeatMeterEntity as DbHeatMeter

// 2. Импортируем нашу чистую доменную КМР-модель для UI слоя
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity as DomainHeatMeter

/**
 * [DomainHeatMeter.toDbHeatMeter] — Конвертация доменной UI-модели в SQLDelight сущность для записи в БД.
 */
fun DomainHeatMeter.toDbHeatMeter(): DbHeatMeter {
  return DbHeatMeter(
    teplomerId = this.teplomerId, // Первичный ключ Long
    number = this.number,
    model = this.model,
    modelId = this.modelId, // Сквозной Long ID модели
    addressId = this.addressId, // Сквозной Long ID адреса квартиры
    edizm = this.edizm,
    koef = this.koef,
    area = this.area, // REAL маппится в Double напрямую
    sdate = this.sdate,
    fpdate = this.fpdate,
    pdate = this.pdate,
    // ИСПРАВЛЕНО: Связываем доменное поле out с экранированным полем isOut твоей SQL схемы
    isOut = this.out.toLong(),
    spisan = this.spisan.toLong(),
    dataSpis = this.dataSpis,
    work = this.work.toLong()
  )
}

/**
 * [DbHeatMeter.toDomainHeatMeter] — Преобразование сущности SQLDelight обратно в чистую доменную модель для UI.
 */
fun DbHeatMeter.toDomainHeatMeter(): DomainHeatMeter {
  return DomainHeatMeter(
    teplomerId = this.teplomerId,
    number = this.number,
    model = this.model,
    modelId = this.modelId,
    addressId = this.addressId,
    edizm = this.edizm,
    koef = this.koef,
    area = this.area,
    sdate = this.sdate,
    fpdate = this.fpdate,
    pdate = this.pdate,
    // ИСПРАВЛЕНО: Безопасное приведение Long из SQLite в Int для кроссплатформенной UI модели
    out = this.isOut.toInt(),
    spisan = this.spisan.toInt(),
    dataSpis = this.dataSpis,
    work = this.work.toInt()
  )
}

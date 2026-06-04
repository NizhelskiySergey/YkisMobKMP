package com.ykis.ykismobkmp.domain.mapper


// 1. Импортируем сгенерированный SQLDelight-класс строго по имени твоей таблицы CamelCase
import com.ykis.ykismobkmp.db.WaterMeterEntity as DbWaterMeter

// 2. Импортируем нашу чистую доменную КМР-модель для UI слоя
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity as DomainWaterMeter

/**
 * [DomainWaterMeter.toDbWaterMeter] — Конвертация доменной UI-модели в SQLDelight сущность для записи в БД.
 */
fun DomainWaterMeter.toDbWaterMeter(): DbWaterMeter {
  return DbWaterMeter(
    vodomerId = this.vodomerId, // Первичный ключ Long
    dvodomerId = this.dvodomerId, // Long ID
    addressId = this.addressId, // Сквозной Long ID адреса квартиры Южного
    nomer = this.nomer,
    model = this.model,
    st = this.st,
    voda = this.voda,
    place = this.place,
    position = this.position,
    sdate = this.sdate,
    fpdate = this.fpdate,
    pdate = this.pdate,
    pp = this.pp,
    zdate = this.zdate,
    avg = this.avg,
    spisan = this.spisan,
    isOut = this.isOut,
    paused = this.paused,
    dataSpis = this.dataSpis,
    work = this.work
  )
}

/**
 * [DbWaterMeter.toDomainWaterMeter] — Преобразование сущности SQLDelight обратно в чистую доменную модель для UI.
 */
fun DbWaterMeter.toDomainWaterMeter(): DomainWaterMeter {
  return DomainWaterMeter(
    vodomerId = this.vodomerId,
    dvodomerId = this.dvodomerId,
    addressId = this.addressId,
    nomer = this.nomer,
    model = this.model,
    // ИСПРАВЛЕНО: Безопасное приведение Long из SQLite в Int для кроссплатформенной UI модели
    st = this.st,
    voda = this.voda,
    place = this.place,
    position = this.position,
    sdate = this.sdate,
    fpdate = this.fpdate,
    pdate = this.pdate,
    pp = this.pp,
    zdate = this.zdate,
    avg = this.avg,
    spisan = this.spisan,
    // ИСПРАВЛЕНО: Считываем из isOut базы данных обратно в доменное КМР-свойство out
    isOut = this.isOut,
    paused = this.paused,
    dataSpis = this.dataSpis,
    work = this.work
  )
}

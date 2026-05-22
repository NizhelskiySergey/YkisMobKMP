package com.ykis.ykismobkmp.domain.mapper

import com.ykis.ykismobkmp.db.WaterReadingEntity as DbWaterReading
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity as DomainWaterReading

/**
 * [DomainWaterReading.toDbWaterReading] — Конвертация доменной UI-модели в SQLDelight сущность для записи в БД.
 */
fun DomainWaterReading.toDbWaterReading(): DbWaterReading {
  return DbWaterReading(
    addressId = this.addressId,
    pokId = this.pokId,
    vodomerId = this.vodomerId,
    dateOt = this.dateOt,
    dateDo = this.dateDo,
    days = this.days,
    lastValue = this.last,
    currentValue = this.current,
    kub = this.kub,
    avg = this.avg,
    pokOt = this.pokOt,
    pokDo = this.pokDo,
    rday = this.rday,
    kubDay = this.kubDay,
    qtyKub = this.qtyKub,
    operator_ = this.operator, // Зарезервированное поле SQLDelight с нижним подчеркиванием
    dateReadings = this.dateReadings,
    tarifXv = this.tarifXv,
    xvoda = this.xvoda,
    tarifvSt = this.tarifvSt,
    stoki = this.stoki,
    dateSt = this.dateSt,
    dateFin = this.dateFin,
    mday = this.mday,
    dateIn = this.dateIn
  )
}

/**
 * [DbWaterReading.toDomainWaterReading] — Преобразование сущности SQLDelight обратно в чистую доменную модель для UI.
 * ИСПРАВЛЕНО НАМЕРТВО: Полная ликвидация .toInt() и .toDouble()! Все типы данных состыкованы один к одному на Long.
 */
fun DbWaterReading.toDomainWaterReading(): DomainWaterReading {
  return DomainWaterReading(
    addressId = this.addressId,
    pokId = this.pokId,
    vodomerId = this.vodomerId,
    dateOt = this.dateOt,
    dateDo = this.dateDo,
    days = this.days,
    last = this.lastValue,
    current = this.currentValue,
    kub = this.kub,
    avg = this.avg,
    pokOt = this.pokOt,
    pokDo = this.pokDo,
    rday = this.rday,
    kubDay = this.kubDay,
    qtyKub = this.qtyKub,
    operator = this.operator_,
    dateReadings = this.dateReadings,
    tarifXv = this.tarifXv,
    xvoda = this.xvoda,
    tarifvSt = this.tarifvSt,
    stoki = this.stoki,
    dateSt = this.dateSt,
    dateFin = this.dateFin,
    mday = this.mday,
    dateIn = this.dateIn
  )
}

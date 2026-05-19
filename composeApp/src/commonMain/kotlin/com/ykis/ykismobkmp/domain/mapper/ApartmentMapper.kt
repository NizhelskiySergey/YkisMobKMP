package com.ykis.ykismobkmp.domain.mapper

// Явно разделяем алиасы сгенерированной таблицы БД и нашей UI доменной сущности
import com.ykis.ykismobkmp.db.ApartmentEntity as DbApartment
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity as DomainApartment

/**
 * [DomainApartment.toDbApartment] — Маппинг доменной сущности в запись СУБД SQLDelight.
 * ИСПРАВЛЕНО: Убраны ложные вызовы .toLong() для полей, которые уже являются Long в домене.
 */
fun DomainApartment.toDbApartment(): DbApartment {
  return DbApartment(
    addressId = this.addressId, // Long -> Long напрямую
    uid = this.uid,
    address = this.address,
    email = this.email,
    kod = this.kod,
    phone = this.phone,
    nanim = this.nanim,
    apartmentOrder = this.order,
    dataOrder = this.dataOrder,

    areaFull = this.areaFull, // Double -> Double напрямую
    areaLife = this.areaLife,
    areaDop = this.areaDop,
    areaBalk = this.areaBalk,
    areaOtopl = this.areaOtopl,

    // Все Int-поля домена переводим в Long СУБД через .toLong()
    tenant = this.tenant.toLong(),
    podnan = this.podnan.toLong(),
    absent = this.absent.toLong(),
    tenantTbo = this.tenantTbo.toLong(),
    room = this.room.toLong(),
    privat = this.privat.toLong(),
    lift = this.lift.toLong(),

    // ИСПРАВЛЕНО: Эти поля уже являются Long в домене ApartmentEntity, убираем .toLong()
    blockId = this.blockId,
    houseId = this.houseId,

    fio = this.fio,

    // Все Int ЖКХ-флаги домена конвертируем в Long СУБД
    subsidia = this.subsidia.toLong(),
    vxvoda = this.vxvoda.toLong(),
    teplomer = this.teplomer.toLong(),
    distributor = this.distributor.toLong(),
    kvartplata = this.kvartplata.toLong(),
    otoplenie = this.otoplenie.toLong(),
    ateplo = this.ateplo.toLong(),
    podogrev = this.podogrev.toLong(),
    voda = this.voda.toLong(),
    stoki = this.stoki.toLong(),
    avoda = this.avoda.toLong(),
    astoki = this.astoki.toLong(),
    tbo = this.tbo.toLong(),

    aggrKv = this.aggrKv.toLong(),
    aggrVoda = this.aggrVoda.toLong(),
    aggrTeplo = this.aggrTeplo.toLong(),
    aggrTbo = this.aggrTbo.toLong(),

    boiler = this.boiler.toLong(),
    enaudit = this.enaudit.toLong(),
    heated = this.heated.toLong(),
    ztp = this.ztp.toLong(),
    ovu = this.ovu.toLong(),
    paused = this.paused.toLong(),
    osmd = this.osmd.toLong(),

    // ИСПРАВЛЕНО: Убран .toLong(), эти ID уже Long
    osmdId = this.osmdId,
    osbb = this.osbb,
    whatChange = this.whatChange,
    dataChange = this.dataChange,
    enauditId = this.enaudit_id,

    tarifKv = this.tarifKv,
    tarifOt = this.tarifOt,
    tarifAot = this.tarifAot,
    tarifGv = this.tarifGv,
    tarifXv = this.tarifXv,
    tarifSt = this.tarifSt,
    tarifTbo = this.tarifTbo,
    tne = this.tne,
    kte = this.kte,
    length = this.length,
    diametr = this.diametr,

    // ИСПРАВЛЕНО: Убран .toLong(), приборы учета имеют тип Long
    dvodomerId = this.dvodomerId,
    dteplomerId = this.dteplomerId,
    operator_ = this.operator,
    dataIn = this.dataIn,
    ipay = this.ipay.toLong(),
    pb = this.pb.toLong(),
    mtb = this.mtb.toLong()
  )
}

/**
 * [DbApartment.toDomainApartment] — Преобразование записи СУБД в доменную сущность UI.
 * ИСПРАВЛЕНО: Удалены ложные .toByte() и .toInt() вызовы, восстановлено Long -> Long сопоставление.
 */
fun DbApartment.toDomainApartment(): DomainApartment {
  return DomainApartment(
    addressId = this.addressId, // Long -> Long напрямую
    uid = this.uid,
    address = this.address,
    email = this.email,
    kod = this.kod,
    phone = this.phone,
    nanim = this.nanim,
    order = this.apartmentOrder,
    dataOrder = this.dataOrder,

    areaFull = this.areaFull,
    areaLife = this.areaLife,
    areaDop = this.areaDop,
    areaBalk = this.areaBalk,
    areaOtopl = this.areaOtopl,

    // Возвращаем Long из базы в Int-поля домена через .toInt()
    tenant = this.tenant.toInt(),
    podnan = this.podnan.toInt(),
    absent = this.absent.toInt(),
    tenantTbo = this.tenantTbo.toInt(),
    room = this.room.toInt(),

    // ИСПРАВЛЕНО: Платформозависимый .toByte() стерт, заменен КМР-совместимым .toInt()
    privat = this.privat.toInt(),
    lift = this.lift.toInt(),

    // ИСПРАВЛЕНО: Убран .toInt(), эти поля в ApartmentEntity имеют сквозной Long тип
    blockId = this.blockId,
    houseId = this.houseId,

    fio = this.fio,

    // ИСПРАВЛЕНО: Все ЖКХ-флаги переводятся из Long базы в Int домена через .toInt()
    subsidia = this.subsidia.toInt(),
    vxvoda = this.vxvoda.toInt(),
    teplomer = this.teplomer.toInt(),
    distributor = this.distributor.toInt(),
    kvartplata = this.kvartplata.toInt(),
    otoplenie = this.otoplenie.toInt(),
    ateplo = this.ateplo.toInt(),
    podogrev = this.podogrev.toInt(),
    voda = this.voda.toInt(),
    stoki = this.stoki.toInt(),
    avoda = this.avoda.toInt(),
    astoki = this.astoki.toInt(),
    tbo = this.tbo.toInt(),

    aggrKv = this.aggrKv.toInt(),
    aggrVoda = this.aggrVoda.toInt(),
    aggrTeplo = this.aggrTeplo.toInt(),
    aggrTbo = this.aggrTbo.toInt(),

    boiler = this.boiler.toInt(),
    enaudit = this.enaudit.toInt(),
    heated = this.heated.toInt(),
    ztp = this.ztp.toInt(),
    ovu = this.ovu.toInt(),
    paused = this.paused.toInt(),
    osmd = this.osmd.toInt(),

    // ИСПРАВЛЕНО: Оставлен чистый Long без кастов к Int
    osmdId = this.osmdId,
    osbb = this.osbb,
    whatChange = this.whatChange,
    dataChange = this.dataChange,
    enaudit_id = this.enauditId,

    tarifKv = this.tarifKv,
    tarifOt = this.tarifOt,
    tarifAot = this.tarifAot,
    tarifGv = this.tarifGv,
    tarifXv = this.tarifXv,
    tarifSt = this.tarifSt,
    tarifTbo = this.tarifTbo,
    tne = this.tne,
    kte = this.kte,
    length = this.length,
    diametr = this.diametr,

    // ИСПРАВЛЕНО: Идентификаторы счетчиков остаются чистым Long
    dvodomerId = this.dvodomerId,
    dteplomerId = this.dteplomerId,
    operator = this.operator_,
    dataIn = this.dataIn,
    ipay = this.ipay.toInt(),
    pb = this.pb.toInt(),
    mtb = this.mtb.toInt()
  )
}

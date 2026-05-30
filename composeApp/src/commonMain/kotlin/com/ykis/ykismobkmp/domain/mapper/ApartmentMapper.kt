package com.ykis.ykismobkmp.domain.mapper

// Явно разделяем алиасы сгенерированной таблицы БД и нашей UI доменной сущности
import com.ykis.ykismobkmp.db.ApartmentEntity as DbApartment
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity as DomainApartment
/**
 * [DomainApartment.toDbApartment] — Маппинг доменной сущности в запись СУБД SQLDelight.
 * ИСПРАВЛЕНО НАМЕРТВО: Вырезаны все вызовы .toLong() для полей, которые стали Long в домене ApartmentEntity!
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

    // ИСПРАВЛЕНО НАМЕРТВО: Вызовы .toLong() удалены, типы совпадают нативно
    tenant = this.tenant,
    podnan = this.podnan,
    absent = this.absent,
    tenantTbo = this.tenantTbo,
    room = this.room,
    privat = this.privat,
    lift = this.lift,

    blockId = this.blockId,
    houseId = this.houseId,

    fio = this.fio,

    subsidia = this.subsidia,
    vxvoda = this.vxvoda,
    teplomer = this.teplomer,
    distributor = this.distributor,
    kvartplata = this.kvartplata,
    otoplenie = this.otoplenie,
    ateplo = this.ateplo,
    podogrev = this.podogrev,
    voda = this.voda,
    stoki = this.stoki,
    avoda = this.avoda,
    astoki = this.astoki,
    tbo = this.tbo,

    aggrKv = this.aggrKv,
    aggrVoda = this.aggrVoda,
    aggrTeplo = this.aggrTeplo,
    aggrTbo = this.aggrTbo,

    boiler = this.boiler,
    enaudit = this.enaudit,
    heated = this.heated,
    ztp = this.ztp,
    ovu = this.ovu,
    paused = this.paused,
    osmd = this.osmd,

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

    dvodomerId = this.dvodomerId,
    dteplomerId = this.dteplomerId,
    operator_ = this.operator,
    dataIn = this.dataIn,
    ipay = this.ipay,
    pb = this.pb,
    mtb = this.mtb
  )
}

/**
 * [DbApartment.toDomainApartment] — Преобразование записи СУБД в доменную сущность UI.
 * ИСПРАВЛЕНО НАМЕРТВО: Полностью удалены деструктивные вызовы .toInt(), восстановлен чистый Long -> Long бесшовный поток.
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

    // ИСПРАВЛЕНО НАМЕРТВО: Вызовы .toInt() вырезаны, домен принимает Long без посредников
    tenant = this.tenant,
    podnan = this.podnan,
    absent = this.absent,
    tenantTbo = this.tenantTbo,
    room = this.room,

    privat = this.privat,
    lift = this.lift,

    blockId = this.blockId,
    houseId = this.houseId,

    fio = this.fio,

    subsidia = this.subsidia,
    vxvoda = this.vxvoda,
    teplomer = this.teplomer,
    distributor = this.distributor,
    kvartplata = this.kvartplata,
    otoplenie = this.otoplenie,
    ateplo = this.ateplo,
    podogrev = this.podogrev,
    voda = this.voda,
    stoki = this.stoki,
    avoda = this.avoda,
    astoki = this.astoki,
    tbo = this.tbo,

    aggrKv = this.aggrKv,
    aggrVoda = this.aggrVoda,
    aggrTeplo = this.aggrTeplo,
    aggrTbo = this.aggrTbo,

    boiler = this.boiler,
    enaudit = this.enaudit,
    heated = this.heated,
    ztp = this.ztp,
    ovu = this.ovu,
    paused = this.paused,
    osmd = this.osmd,

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

    dvodomerId = this.dvodomerId,
    dteplomerId = this.dteplomerId,
    operator = this.operator_,
    dataIn = this.dataIn,
    ipay = this.ipay,
    pb = this.pb,
    mtb = this.mtb
  )
}


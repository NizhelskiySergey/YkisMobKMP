package com.ykis.ykismobkmp.domain.mapper

import com.ykis.ykismobkmp.db.ApartmentEntity as DbApartment
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity as DomainApartment

/**
 * [DomainApartment.toDbApartment] — Маппинг домена в БД (Все Int/Byte переводятся в Long).
 */
fun DomainApartment.toDbApartment(): DbApartment {
  return DbApartment(
    addressId = this.addressId,
    uid = this.uid,
    address = this.address,
    email = this.email,
    kod = this.kod,
    phone = this.phone,
    nanim = this.nanim,
    apartmentOrder = this.order,
    dataOrder = this.dataOrder,

    areaFull = this.areaFull,
    areaLife = this.areaLife,
    areaDop = this.areaDop,
    areaBalk = this.areaBalk,
    areaOtopl = this.areaOtopl,

    // Все Int и Byte поля переводим в Long через .toLong()
    tenant = this.tenant.toLong(),
    podnan = this.podnan.toLong(),
    absent = this.absent.toLong(),
    tenantTbo = this.tenantTbo.toLong(),
    room = this.room.toLong(),
    privat = this.privat.toLong(),
    lift = this.lift.toLong(),
    blockId = this.blockId.toLong(),
    houseId = this.houseId,

    fio = this.fio,

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
    osmdId = this.osmdId.toLong(),
    osbb = this.osbb,
    whatChange = this.whatChange,
    dataChange = this.dataChange,
    enauditId = this.enaudit_id.toLong(),

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

    dvodomerId = this.dvodomerId.toLong(),
    dteplomerId = this.dteplomerId.toLong(),
    operator_ = this.operator,
    dataIn = this.dataIn,
    ipay = this.ipay.toLong(),
    pb = this.pb.toLong(),
    mtb = this.mtb.toLong()
  )
}

/**
 * [DbApartment.toDomainApartment] — Превращаем Long из БД обратно в Int/Byte для домена.
 */
fun DbApartment.toDomainApartment(): DomainApartment{
  return DomainApartment(
    addressId = this.addressId,
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

    // Переводим Long из базы обратно в Int через .toInt()
    tenant = this.tenant.toInt(),
    podnan = this.podnan.toInt(),
    absent = this.absent.toInt(),
    tenantTbo = this.tenantTbo.toInt(),
    room = this.room.toInt(),

    // Переводим Long из базы в Byte через .toByte()
    privat = this.privat.toByte(),
    lift = this.lift.toByte(),
    blockId = this.blockId.toInt(),
    houseId = this.houseId,

    fio = this.fio,

    subsidia = this.subsidia.toByte(),
    vxvoda = this.vxvoda.toByte(),
    teplomer = this.teplomer.toByte(),
    distributor = this.distributor.toByte(),
    kvartplata = this.kvartplata.toByte(),
    otoplenie = this.otoplenie.toByte(),
    ateplo = this.ateplo.toByte(),
    podogrev = this.podogrev.toByte(),
    voda = this.voda.toByte(),
    stoki = this.stoki.toByte(),
    avoda = this.avoda.toByte(),
    astoki = this.astoki.toByte(),
    tbo = this.tbo.toByte(),

    aggrKv = this.aggrKv.toByte(),
    aggrVoda = this.aggrVoda.toByte(),
    aggrTeplo = this.aggrTeplo.toByte(),
    aggrTbo = this.aggrTbo.toByte(),

    boiler = this.boiler.toByte(),
    enaudit = this.enaudit.toInt(),
    heated = this.heated.toByte(),
    ztp = this.ztp.toByte(),
    ovu = this.ovu.toByte(),
    paused = this.paused.toByte(),
    osmd = this.osmd.toByte(),
    osmdId = this.osmdId.toInt(),
    osbb = this.osbb,
    whatChange = this.whatChange,
    dataChange = this.dataChange,
    enaudit_id = this.enauditId.toInt(),

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

    dvodomerId = this.dvodomerId.toInt(),
    dteplomerId = this.dteplomerId.toInt(),
    operator = this.operator_,
    dataIn = this.dataIn,
    ipay = this.ipay.toInt(),
    pb = this.pb.toInt(),
    mtb = this.mtb.toInt()
  )
}

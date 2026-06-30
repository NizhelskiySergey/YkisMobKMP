package com.ykis.ykismobkmp.domain.mapper

import com.ykis.ykismobkmp.db.FamilyEntity as DbFamily
import com.ykis.ykismobkmp.domain.entity.FamilyEntity as DomainFamily

/**
 * [DomainFamily.toDbFamily] — Преобразование доменной модели в сущность SQLDelight.
 * Все Int поля приводятся к системному Long (INTEGER) для SQLite на Mac и Android.
 */
fun DomainFamily.toDbFamily(): DbFamily {
  return DbFamily(
    recId = this.recId,
    addressId = this.addressId,
    rodstvo = this.rodstvo,
    firstname = this.fistname, // Связываем с твоей опечаткой fistname в домене
    lastname = this.lastname,
    surname = this.surname,
    born = this.born,
    sex = this.sex,
    phone = this.phone,
    subsidia = this.subsidia,
    vkl = this.vkl,
    inn = this.inn,
    document = this.document,
    seria = this.seria,
    nomer = this.nomer,
    datav = this.datav,
    organ = this.organ
  )
}

/**
 * [DbFamily.toDomainFamily] — Преобразование сущности SQLDelight обратно в доменную модель.
 * Возвращает Long типы базы данных к исходным Int типам бизнес-логики.
 */
fun DbFamily.toDomainFamily(): DomainFamily {
  return DomainFamily(
    recId = this.recId,
    addressId = this.addressId,
    rodstvo = this.rodstvo,
    fistname = this.firstname, // Возвращаем в доменное поле fistname
    lastname = this.lastname,
    surname = this.surname,
    born = this.born,
    sex = this.sex,
    phone = this.phone,
    subsidia = this.subsidia,
    vkl = this.vkl,
    inn = this.inn,
    document = this.document,
    seria = this.seria,
    nomer = this.nomer,
    datav = this.datav,
    organ = this.organ
  )
}

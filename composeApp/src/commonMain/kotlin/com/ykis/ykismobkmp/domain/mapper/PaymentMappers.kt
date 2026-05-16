package com.ykis.ykismobkmp.domain.mapper


// 1. Импортируем сгенерированный SQLDelight-класс строго по имени твоей таблицы CamelCase
import com.ykis.ykismobkmp.db.PaymentEntity as DbPayment

// 2. Импортируем нашу чистую доменную КМР-модель для UI слоя
import com.ykis.ykismobkmp.domain.entity.PaymentEntity as DomainPayment

/**
 * [DomainPayment.toDbPayment] — Конвертация доменной UI-модели в SQLDelight сущность для записи в БД.
 */
fun DomainPayment.toDbPayment(): DbPayment {
  return DbPayment(
    recId = this.recID,       // Первичный ключ Long
    addressId = this.addressId, // Внешний связующий ключ Long
    data_ = this.data,
    kvartplata = this.kvartplata, // REAL в схеме СУБД идеально ложится в Double
    remont = this.remont,
    otoplenie = this.otoplenie,
    voda = this.voda,
    tbo = this.tbo,
    summa = this.summa,
    prixod = this.prixod,
    kassa = this.kassa,
    nomer = this.nomer,
    dataIn = this.dataIn
  )
}

/**
 * [DbPayment.toDomainPayment] — Преобразование сущности SQLDelight обратно в чистую доменную модель для UI.
 */
fun DbPayment.toDomainPayment(): DomainPayment {
  return DomainPayment(
    recID = this.recId,
    addressId = this.addressId,
    data = this.data_,
    kvartplata = this.kvartplata,
    remont = this.remont,
    otoplenie = this.otoplenie,
    voda = this.voda,
    tbo = this.tbo,
    summa = this.summa,
    prixod = this.prixod,
    kassa = this.kassa,
    nomer = this.nomer,
    dataIn = this.dataIn
  )
}

package com.ykis.ykismobkmp.domain.mapper

import com.ykis.ykismobkmp.domain.entity.ServiceEntity as DomainService
import com.ykis.ykismobkmp.db.ServiceEntity as DbService // Имя сгенерированного SQLDelight класса строки
private const val className = "ServiceMapper"

/**
 * [toDomainService] — Преобразование автогенерированной строки SQLDelight таблицы в чистую доменную модель UI слоя.
 */
fun DbService.toDomainService(): DomainService {
  println("[$className.toDomainService]: Конвертація локальних кЕш-даних СУБД для Л/С ID: ${this.addressId}")

  return DomainService(
    addressId = this.addressId,
    service = this.service,
    service1 = this.service1,
    service2 = this.service2,
    service3 = this.service3,
    service4 = this.service4,
    // ИСПРАВЛЕНО НАМЕРТВО: Вычитываем дату из новой чистой колонки dateNach вместо старой data_
    data = this.dateNach,
    zadol = this.zadol,
    zadol1 = this.zadol1,
    zadol2 = this.zadol2,
    zadol3 = this.zadol3,
    zadol4 = this.zadol4,
    nachisleno = this.nachisleno,
    nachisleno1 = this.nachisleno1,
    nachisleno2 = this.nachisleno2,
    nachisleno3 = this.nachisleno3,
    nachisleno4 = this.nachisleno4,
    oplacheno = this.oplacheno,
    oplacheno1 = this.oplacheno1,
    oplacheno2 = this.oplacheno2,
    oplacheno3 = this.oplacheno3,
    oplacheno4 = this.oplacheno4,
    dolg = this.dolg,
    dolg1 = this.dolg1,
    dolg2 = this.dolg2,
    dolg3 = this.dolg3,
    dolg4 = this.dolg4
  )
}

/**
 * [toDbService] — Маршалинг доменной UI структуры сетевого ответа Ktor в плоскую SQLite-строку таблицы SQLDelight.
 */
fun DomainService.toDbService(): DbService {
  println("[$className.toDbService]: Пакування мережевого JSON пакета в SQLite рядок для Л/С ID: ${this.addressId}")

  return DbService(
    addressId = this.addressId,
    service = this.service,
    service1 = this.service1 ?: "Unknown",
    service2 = this.service2 ?: "Unknown",
    service3 = this.service3 ?: "Unknown",
    service4 = this.service4 ?: "Unknown",
    // ИСПРАВЛЕНО НАМЕРТВО: Перекладываем доменную дату из Ktor-сети в целевое поле dateNach на диске,
    // полностью ликвидируя схлопывание истории и мертвые клины компилятора!
    dateNach = this.data,
    zadol = this.zadol ?: 0.0,
    zadol1 = this.zadol1 ?: 0.0,
    zadol2 = this.zadol2 ?: 0.0,
    zadol3 = this.zadol3 ?: 0.0,
    zadol4 = this.zadol4 ?: 0.0,
    nachisleno = this.nachisleno ?: 0.0,
    nachisleno1 = this.nachisleno1 ?: 0.0,
    nachisleno2 = this.nachisleno2 ?: 0.0,
    nachisleno3 = this.nachisleno3 ?: 0.0,
    nachisleno4 = this.nachisleno4 ?: 0.0,
    oplacheno = this.oplacheno ?: 0.0,
    oplacheno1 = this.oplacheno1 ?: 0.0,
    oplacheno2 = this.oplacheno2 ?: 0.0,
    oplacheno3 = this.oplacheno3 ?: 0.0,
    oplacheno4 = this.oplacheno4 ?: 0.0,
    dolg = this.dolg ?: 0.0,
    dolg1 = this.dolg1 ?: 0.0,
    dolg2 = this.dolg2 ?: 0.0,
    dolg3 = this.dolg3 ?: 0.0,
    dolg4 = this.dolg4 ?: 0.0
  )
}


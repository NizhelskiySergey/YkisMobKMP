package com.ykis.ykismobkmp.cash.ledger

import com.ykis.ykismobkmp.cash.sqlDelight.LedgerDao
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.entity.FastpayEntity

/**
 * [LedgerRepositoryCashImpl] — Реализация кэша Ledger на одиночных идентификаторах Long.
 */
class LedgerRepositoryCashImpl(
  private val ledgerDao: LedgerDao
) : LedgerRepositoryCash {

  private val className = "LedgerRepositoryCashImpl"

  override suspend fun addService(service: List<ServiceEntity>) {
    println("[$className.addService]: Пакетне збереження ${service.size} записів нарахувань через LedgerDao...")
    ledgerDao.insertService(service)
  }

  override suspend fun getServiceDetail(addressId: Long, service: String, year: String): List<ServiceEntity> {
    println("[$className.getServiceDetail]: Запит локальної історії з LedgerDao для о/р: $addressId, Служба: $service, Рік: $year")
    return ledgerDao.getServiceDetail(addressId, service, year)
  }

  override suspend fun deleteAllService() {
    println("[$className.deleteAllService]: Повне очищення всієї таблиці СУБД...")
    ledgerDao.deleteAllService()
  }

  override suspend fun getTotalDebt(addressId: Long): ServiceEntity? {
    println("[$className.getTotalDebt]: Вибірка зведеного балансу для о/р: $addressId")
    return ledgerDao.getTotalDebt(addressId)
  }

  override suspend fun deleteServiceByApartment(addressId: Long) {
    println("[$className.deleteServiceByApartment]: Зачистка кЕшу нарахувань для о/р: $addressId")
    ledgerDao.deleteServiceByApartment(addressId)
  }

  override suspend fun insertFastpayTokens(tokens: List<FastpayEntity>) {
    println("[$className.insertFastpayTokens]: Пакетне збереження ${tokens.size} токенів оплати...")
    ledgerDao.insertFastpayTokens(tokens)
  }

  override suspend fun getFastpayTokens(): List<FastpayEntity> {
    println("[$className.getFastpayTokens]: Запит токенів оплати з локальної СУБД...")
    return ledgerDao.getFastpayTokens()
  }
}


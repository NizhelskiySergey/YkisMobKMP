package com.ykis.ykismobkmp.cash.sqlDelight

import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import com.ykis.ykismobkmp.domain.entity.RaionEntity
import com.ykis.ykismobkmp.domain.entity.HouseEntity
import com.ykis.ykismobkmp.domain.mapper.toDbApartment
import com.ykis.ykismobkmp.domain.mapper.toDbFamily
import com.ykis.ykismobkmp.domain.mapper.toDbHouse
import com.ykis.ykismobkmp.domain.mapper.toDbRaion
import com.ykis.ykismobkmp.domain.mapper.toDomainApartment
import com.ykis.ykismobkmp.domain.mapper.toDomainFamily
import com.ykis.ykismobkmp.domain.mapper.toDomainHouse
import com.ykis.ykismobkmp.domain.mapper.toDomainRaion
import com.ykis.ykismobkmp.db.DatabaseSchemaInitializer

/**
 * [ApartmentDao] — КМР-класс доступа к запросам SQLDelight.
 */
class ApartmentDao(
  private val dbQueries: YkisDatabasesQueries,
  private val driver: SqlDriver,
  private val schemaInitializer: DatabaseSchemaInitializer
) {
  private val className = "ApartmentDao"

  private suspend fun ensureSchema() {
    schemaInitializer.ensureSchema(driver)
  }

  suspend fun insertApartments(apartments: List<ApartmentEntity>) {
    println("[$className]: Старт insertApartments. Кількість: ${apartments.size}")
    ensureSchema()
    try {
        println("[$className]: Відкриття транзакції СУБД...")
        dbQueries.transaction {
          apartments.forEach { apartment ->
            dbQueries.insertApartment(apartment.toDbApartment())
          }
        }
        println("[$className]: Запис завершено успішно.")
    } catch (e: Exception) {
        println("[${className}_ERROR]: Помилка: ${e.message}")
        throw e
    }
  }

  suspend fun syncFullDatabase(apartments: List<ApartmentEntity>) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteAllApartments()
      dbQueries.deleteAllFamily()
      dbQueries.deleteAllWaterMeters()
      dbQueries.deleteAllHeatMeters()
      dbQueries.deleteAllService()
      dbQueries.deleteAllPayment()
      dbQueries.deleteAllWaterReadings()
      dbQueries.deleteAllHeatReadings()

      apartments.forEach { apartment ->
        dbQueries.insertApartment(apartment.toDbApartment())
      }
    }
  }

  suspend fun deleteAllApartments() {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteAllApartments()
    }
  }

  suspend fun deleteFlat(addressId: Long) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteFlat(addressId = addressId)
    }
  }

  suspend fun getFlatById(addressId: Long): ApartmentEntity? {
    ensureSchema()
    return dbQueries.getFlatById(addressId)
      .awaitAsOneOrNull()
      ?.toDomainApartment()
  }

  suspend fun getApartmentList(): List<ApartmentEntity> {
    ensureSchema()
    return dbQueries.getApartmentList()
      .awaitAsList()
      .map { it.toDomainApartment() }
  }

  suspend fun getApartmentsByUser(): List<ApartmentEntity> {
    ensureSchema()
    return dbQueries.getApartmentList()
      .awaitAsList()
      .map { it.toDomainApartment() }
  }

  suspend fun getRaionList(): List<RaionEntity> {
    ensureSchema()
    return dbQueries.getRaionList()
      .awaitAsList()
      .map { it.toDomainRaion() }
  }

  suspend fun syncRaionList(raions: List<RaionEntity>) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteAllRaions()
      raions.forEach { raion ->
        dbQueries.insertRaion(raion.toDbRaion())
      }
    }
  }

  suspend fun getHousesByRaion(raionId: Long): List<HouseEntity> {
    ensureSchema()
    return dbQueries.getHousesByRaion(raionId)
      .awaitAsList()
      .map { it.toDomainHouse() }
  }

  suspend fun syncHouseList(houses: List<HouseEntity>) {
    ensureSchema()
    dbQueries.transaction {
      val currentRaionId = houses.firstOrNull()?.raionId
      if (currentRaionId != null && currentRaionId != 0L) {
        dbQueries.deleteHousesByRaionId(currentRaionId)
      }
      houses.forEach { house ->
        dbQueries.insertHouse(house.toDbHouse())
      }
    }
  }

  suspend fun getFamilyByApartment(addressId: Long): List<FamilyEntity> {
    ensureSchema()
    return dbQueries.getFamilyByApartment(addressId)
      .awaitAsList()
      .map { it.toDomainFamily() }
  }

  suspend fun syncFamilyList(addressId: Long, familyList: List<FamilyEntity>) {
    if (addressId <= 0L) return
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteFamilyByAddressId(addressId)
      if (familyList.isNotEmpty()) {
        familyList.forEach { member ->
          dbQueries.insertFamily(member.toDbFamily())
        }
      }
    }
  }

  suspend fun addFamilyByUser(family: List<FamilyEntity>) {
    ensureSchema()
    dbQueries.transaction {
      family.forEach { member ->
        dbQueries.insertFamily(member.toDbFamily())
      }
    }
  }

  suspend fun deleteAllFamily() {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteAllFamily()
    }
  }

  suspend fun deleteFamilyByApartment(addressIds: List<Long>) {
    ensureSchema()
    dbQueries.transaction {
      addressIds.forEach { id ->
        dbQueries.deleteFamilyByAddressId(id)
      }
    }
  }
}

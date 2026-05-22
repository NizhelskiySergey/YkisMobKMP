package com.ykis.ykismobkmp.cash.sqlDelight

import com.ykis.ykismobkmp.db.YkisDatabasesQueries
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

/**
 * [ApartmentDao] — КМР-класс доступа к сгенерированному интерфейсу запросов SQLDelight 2.x.
 * ИСПРАВЛЕНО: Префикс логирования изменен на YkisLogKMP.
 */
class ApartmentDao(
  private val dbQueries: YkisDatabasesQueries
) {
  private val className = "ApartmentDao"

  suspend fun insertApartments(apartments: List<ApartmentEntity>) {
    dbQueries.transaction {
      apartments.forEach { apartment ->
        dbQueries.insertApartment(apartment.toDbApartment())
      }
    }
  }

  suspend fun syncFullDatabase(apartments: List<ApartmentEntity>) {
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
    dbQueries.deleteAllApartments()
  }

  suspend fun deleteFlat(addressId: Long) {
    dbQueries.deleteFlat(addressId = addressId)
  }

  suspend fun getFlatById(addressId: Long): ApartmentEntity? {
    return dbQueries.getFlatById(addressId).executeAsOneOrNull()?.toDomainApartment()
  }

  suspend fun getApartmentList(): List<ApartmentEntity> {
    return dbQueries.getApartmentList().executeAsList().map { it.toDomainApartment() }
  }

  suspend fun getRaionList(): List<RaionEntity> {
    return dbQueries.getRaionList().executeAsList().map { it.toDomainRaion() }
  }

  suspend fun syncRaionList(raions: List<RaionEntity>) {
    dbQueries.transaction {
      dbQueries.deleteAllRaions()
      raions.forEach { raion ->
        dbQueries.insertRaion(raion.toDbRaion())
      }
    }
  }

  suspend fun getHousesByRaion(raionId: Long): List<HouseEntity> {
    return dbQueries.getHousesByRaion(raionId).executeAsList().map { it.toDomainHouse() }
  }

  suspend fun syncHouseList(houses: List<HouseEntity>) {
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
    return dbQueries.getFamilyByApartment(addressId).executeAsList().map { it.toDomainFamily() }
  }

  suspend fun syncFamilyList(addressId: Long, familyList: List<FamilyEntity>) {
    if (addressId <= 0L) {
      println("[YkisLogKMP.$className.syncFamilyList]: [ABORT] Попытка вызова с невалидным addressId=$addressId")
      return
    }

    dbQueries.transaction {
      dbQueries.deleteFamilyByAddressId(addressId)
      println("[YkisLogKMP.$className.syncFamilyList]: Локальный кэш семьи зачищен для квартиры ID=$addressId")

      if (familyList.isNotEmpty()) {
        familyList.forEach { member ->
          dbQueries.insertFamily(member.toDbFamily())
        }
        println("[YkisLogKMP.$className.syncFamilyList]: Записано ${familyList.size} новых членов семьи в SQLDelight")
      }
    }
  }

  suspend fun addFamilyByUser(family: List<FamilyEntity>) {
    dbQueries.transaction {
      family.forEach { member ->
        dbQueries.insertFamily(member.toDbFamily())
      }
    }
  }

  suspend fun deleteAllFamily() {
    dbQueries.deleteAllFamily()
  }

  suspend fun deleteFamilyByApartment(addressIds: List<Long>) {
    dbQueries.transaction {
      addressIds.forEach { id ->
        dbQueries.deleteFamilyByAddressId(id)
      }
      println("[YkisLogKMP.$className.deleteFamilyByApartment]: Успешно зачищен кэш семей для пачки ID: $addressIds")
    }
  }
}

package com.ykis.ykismobkmp.cash.apartment

import com.ykis.ykismobkmp.cash.sqlDelight.ApartmentDao
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import com.ykis.ykismobkmp.domain.entity.HouseEntity
import com.ykis.ykismobkmp.domain.entity.RaionEntity

/**
 * [ApartmentCacheImpl] — Чистая КМР-реализация кэша через объекты доступа к данным SQLDelight.
 */
class ApartmentCacheImpl(
  private val apartmentDao: ApartmentDao
) : ApartmentCache {

  // ==========================================
  // ЛОГИКА УПРАВЛЕНИЯ КВАРТИРАМИ (APARTMENT)
  // ==========================================

  override suspend fun insertApartmentList(apartment: List<ApartmentEntity>) {
    // Вызываем точное имя метода множественной вставки из твоего ApartmentDao
    apartmentDao.insertApartments(apartment)
  }

  override suspend fun getApartmentsByUser(): List<ApartmentEntity> {
    return apartmentDao.getApartmentList()
  }

  override suspend fun syncFullDatabase(apartments: List<ApartmentEntity>) {
    apartmentDao.syncFullDatabase(apartments)
  }

  override suspend fun deleteAllApartments() {
    apartmentDao.deleteAllApartments()
  }

  override suspend fun deleteFlat(addressId: Long) {
    apartmentDao.deleteFlat(addressId)
  }

  override suspend fun getApartmentById(addressId: Long): ApartmentEntity? {
    return apartmentDao.getFlatById(addressId)
  }

  // ==========================================
  // ЛОГИКА УПРАВЛЕНИЯ РАЙОНАМИ (RAION)
  // ==========================================

  override suspend fun getRaionList(): List<RaionEntity> {
    return apartmentDao.getRaionList()
  }

  override suspend fun syncRaionList(raions: List<RaionEntity>) {
    apartmentDao.syncRaionList(raions)
  }

  // ==========================================
  // ЛОГИКА УПРАВЛЕНИЯ ДОМАМИ (HOUSE)
  // ==========================================

  override suspend fun getHousesByRaion(raionId: Long): List<HouseEntity> {
    return apartmentDao.getHousesByRaion(raionId)
  }

  override suspend fun syncHouseList(houses: List<HouseEntity>) {
    apartmentDao.syncHouseList(houses)
  }

  // ==========================================
  // ЛОГИКА УПРАВЛЕНИЯ СОСТАВОМ СЕМЬИ (FAMILY)
  // ==========================================

  override suspend fun addFamilyByUser(family: List<FamilyEntity>) {
    apartmentDao.addFamilyByUser(family)
  }

  override suspend fun getFamilyByApartment(addressId: Long): List<FamilyEntity> {
    return apartmentDao.getFamilyByApartment(addressId)
  }

  override suspend fun syncFamilyList(addressId: Long, familyList: List<FamilyEntity>) {
    apartmentDao.syncFamilyList(addressId, familyList)
  }

  override suspend fun deleteAllFamily() {
    apartmentDao.deleteAllFamily()
  }

  override suspend fun deleteFamilyByApartment(addressIds: List<Long>) {
    // Передаем сквозную коллекцию List<Long> в зафиксированный метод ApartmentDao
    apartmentDao.deleteFamilyByApartment(addressIds)
  }
}


package com.ykis.ykismobkmp.cash.apartment

import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import com.ykis.ykismobkmp.domain.entity.HouseEntity
import com.ykis.ykismobkmp.domain.entity.RaionEntity

/**
 * [ApartmentCache] — Главный КМР-контракт локального хранилища баз данных ЮКИС.
 */
interface ApartmentCache {

  // ==========================================
  // ЛОГИКА УПРАВЛЕНИЯ КВАРТИРАМИ (APARTMENT)
  // ==========================================
  suspend fun insertApartmentList(apartment: List<ApartmentEntity>)
  suspend fun getApartmentsByUser(): List<ApartmentEntity>
  suspend fun syncFullDatabase(apartments: List<ApartmentEntity>)
  suspend fun deleteAllApartments()
  suspend fun deleteFlat(addressId: Long)
  suspend fun getApartmentById(addressId: Long): ApartmentEntity?

  // ==========================================
  // ЛОГИКА УПРАВЛЕНИЯ РАЙОНАМИ (RAION)
  // ==========================================
  suspend fun getRaionList(): List<RaionEntity>
  suspend fun syncRaionList(raions: List<RaionEntity>)

  // ==========================================
  // ЛОГИКА УПРАВЛЕНИЯ ДОМАМИ (HOUSE)
  // ==========================================
  suspend fun getHousesByRaion(raionId: Long): List<HouseEntity>
  suspend fun syncHouseList(houses: List<HouseEntity>)

  // ==========================================
  // ЛОГИКА УПРАВЛЕНИЯ СОСТАВОМ СЕМЬИ (FAMILY)
  // ==========================================
  suspend fun addFamilyByUser(family: List<FamilyEntity>)
  suspend fun getFamilyByApartment(addressId: Long): List<FamilyEntity>

  /**
   * Атомарная синхронизация состава семьи.
   * @param addressId жесткий входной идентификатор квартиры для локализации транзакции удаления и вставки.
   * @param familyList свежий список родственников с сервера (может быть пустым).
   */
  suspend fun syncFamilyList(addressId: Long, familyList: List<FamilyEntity>)

  suspend fun deleteAllFamily()

  /**
   * Пакетное каскадное удаление состава семьи для набора квартир.
   * ИСПРАВЛЕНО НАМЕРТВО: Изменено имя параметра на addressIds и тип на List<Long> под сквозной стандарт KMP!
   */
  suspend fun deleteFamilyByApartment(addressIds: List<Long>)
}

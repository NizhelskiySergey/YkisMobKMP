package com.ykis.ykismobkmp.data.remote.apartment


import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetFamilyResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse

/**
 * [ApartmentRemote] — Интерфейс удаленного сетевого взаимодействия с API ЮКИС.
 * ИСПРАВЛЕНО НАМЕРТВО: Все целочисленные идентификаторы ID переведены на тип Long!
 * Намертво зафиксирован для полной замены.
 */
interface ApartmentRemote {
  suspend fun getApartmentList(uid: String): GetApartmentsResponse
  suspend fun getOsbbApartmentsList(targetId: Long, isHouse: Boolean): GetApartmentsResponse
  suspend fun getRaionList(uid: String): GetRaionsResponse
  suspend fun getHouseByRaionList(raionId: Long): GetHousesResponse
  suspend fun updateBti(addressId: Long,phone: String,email: String): GetApartmentsResponse
  suspend fun getApartment(addressId: Long, uid: String): GetApartmentResponse
  suspend fun deleteApartment(addressId: Long, uid: String): GetSimpleResponse
  suspend fun addApartment(code: String, uid: String, email: String): GetSimpleResponse
  suspend fun verifyAdminSecretWord(code: String, uid: String): GetSimpleResponse
  suspend fun saveUserUid(uid: String, email: String): GetSimpleResponse
  suspend fun deleteUserAccount(uid: String, email: String): GetSimpleResponse
  suspend fun getFamilyList(uid: String, addressId: Long): GetFamilyResponse
}

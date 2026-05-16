package com.ykis.ykismobkmp.domain.repository.apartment

import com.ykis.ykismobkmp.data.responses.BaseResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetFamilyResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity

interface ApartmentRepository {
  suspend fun getApartmentList(uid: String): GetApartmentsResponse
  suspend fun updateBti(params: ApartmentEntity): GetSimpleResponse
  suspend fun getApartment( uid: String,addressId: Long): GetApartmentResponse
  suspend fun deleteApartment( uid: String,addressId: Long): GetSimpleResponse
  suspend fun addApartmentUser( uid: String,code: String, email: String): GetSimpleResponse
  suspend fun verifyAdminSecretWord(uid: String,code: String): GetSimpleResponse
  suspend fun saveUserUid(uid: String,email: String): GetSimpleResponse
  suspend fun deleteUserAccount(uid: String,email: String): GetSimpleResponse
  // В ApartmentRepository
  suspend fun getOsbbApartmentsList(targetId: Long, isHouse: Boolean): GetApartmentsResponse // Твой формат ответа
  suspend fun getRaionList(uid: String): GetRaionsResponse // Твой формат ответа
  suspend fun getHouseByRaionList(raionId: Long): GetHousesResponse // Твой формат ответа
  suspend fun getFamilyList(uid: String, addressId: Long): GetFamilyResponse

}

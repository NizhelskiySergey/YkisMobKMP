package com.ykis.ykismobkmp.domain.repository.apartment

import com.ykis.ykismobkmp.data.responses.BaseResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity

interface ApartmentRepository {
  suspend fun getApartmentList(uid: String): GetApartmentsResponse
  suspend fun updateBti(params: ApartmentEntity): BaseResponse
  suspend fun getApartment(addressId: Long, uid: String): GetApartmentResponse
  suspend fun deleteApartment(addressId: Long, uid: String): GetSimpleResponse
  suspend fun addApartmentUser(code: String, uid: String, email: String): GetSimpleResponse
  suspend fun verifyAdminSecretWord(code: String, uid: String): GetSimpleResponse
  suspend fun saveUserUid(uid: String,email: String): GetSimpleResponse
  suspend fun deleteUserAccount(uid: String,email: String): GetSimpleResponse
  // В ApartmentRepository
  suspend fun getOsbbApartmentsList(targetId: Long, isHouse: Boolean): GetApartmentsResponse // Твой формат ответа
  suspend fun getRaionList(uid: String): GetRaionsResponse // Твой формат ответа
  suspend fun getHouseByRaionList(raionId: Long): GetHousesResponse // Твой формат ответа

}

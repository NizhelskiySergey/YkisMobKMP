package com.ykis.ykismobkmp.data.remote.apartment

import com.ykis.ykismobkmp.core.Constants.ADDRESS_ID
import com.ykis.ykismobkmp.core.Constants.CODE
import com.ykis.ykismobkmp.core.Constants.EMAIL
import com.ykis.ykismobkmp.core.Constants.PHONE
import com.ykis.ykismobkmp.core.Constants.RAION_ID
import com.ykis.ykismobkmp.core.Constants.UID
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetFamilyResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse

/**
 * [ApartmentRemoteImpl] — Реализация удаленного репозитория на базе KtorApiService.
 */
class ApartmentRemoteImpl(
  private val ktorApiService: KtorApiService
) : ApartmentRemote {

  override suspend fun getApartmentList(uid: String): GetApartmentsResponse {
    return ktorApiService.getApartmentList(createGetApartmentListMap(uid))
  }

  override suspend fun getOsbbApartmentsList(targetId: Long, isHouse: Boolean): GetApartmentsResponse {
    return ktorApiService.getOsbbApartmentsList(createGetOsbbApartmentsListMap(targetId, isHouse))
  }

  override suspend fun getHouseByRaionList(raionId: Long): GetHousesResponse {
    return ktorApiService.getHouseByRaionList(createGetHouseByRaionListMap(raionId))
  }

  override suspend fun getRaionList(uid: String): GetRaionsResponse {
    return ktorApiService.getRaionList(createGetRaionListMap(uid))
  }


  override suspend fun getApartment(addressId: Long, uid: String): GetApartmentResponse {
    return ktorApiService.getApartment(createRequestByAddressId(addressId = addressId, uid = uid))
  }
  override suspend fun updateBti(addressId: Long, phone: String, email: String): GetApartmentsResponse {
    return ktorApiService.updateBti(createRequestUpdateBti(addressId = addressId, phone = phone,email=email))
  }
  override suspend fun deleteApartment(addressId: Long, uid: String): GetSimpleResponse {
    return ktorApiService.deleteApartment(createRequestByAddressId(addressId, uid))
  }

  override suspend fun addApartment(code: String, uid: String, email: String): GetSimpleResponse {
    return ktorApiService.addApartment(createAddApartmentMap(code = code, uid = uid, email = email))
  }

  override suspend fun verifyAdminSecretWord(code: String, uid: String): GetSimpleResponse {
    return ktorApiService.verifyAdminSecretWord(createVerifyAdminSecretWord(code = code, uid = uid))
  }

  override suspend fun saveUserUid(uid: String, email: String): GetSimpleResponse {
    return ktorApiService.saveUserUid(createSaveUserUid(uid = uid, email = email))
  }

  override suspend fun deleteUserAccount(uid: String, email: String): GetSimpleResponse {
    return ktorApiService.deleteUserAccount(createDeleteUserAccount(uid = uid, email = email))
  }

  override suspend fun getFamilyList(uid: String, addressId: Long): GetFamilyResponse {
    return ktorApiService.getFamilyList(
      createGetFamilyListMap(
        addressId = addressId,
        uid = uid
      )
    )
  }

  private fun createGetFamilyListMap(addressId: Long, uid: String): Map<String, String> {
    val map = HashMap<String, String>()
    map[ADDRESS_ID] = addressId.toString()
    map[UID] = uid
    return map
  }

  private fun createGetApartmentListMap(uid: String): Map<String, String> {
    val map = HashMap<String, String>()
    map[UID] = uid
    return map
  }

  private fun createGetRaionListMap(uid: String): Map<String, String> {
    val map = HashMap<String, String>()
    map[UID] = uid
    return map
  }

  private fun createGetOsbbApartmentsListMap(targetId: Long, isHouse: Boolean): Map<String, String> {
    val map = HashMap<String, String>()
    map["targetId"] = targetId.toString()
    map["isHouse"] = if (isHouse) "1" else "0"
    return map
  }

  private fun createGetHouseByRaionListMap(raionId: Long): Map<String, String> {
    val map = HashMap<String, String>()
    map[RAION_ID] = raionId.toString()
    return map
  }

  private fun createRequestByAddressId(addressId: Long, uid: String): Map<String, String> {
    val map = HashMap<String, String>()
    map[ADDRESS_ID] = addressId.toString()
    map[UID] = uid
    return map
  }

  private fun createRequestUpdateBti(addressId: Long, phone: String, email: String): Map<String, String> {
    val map = HashMap<String, String>()
    map[ADDRESS_ID] = addressId.toString()
    map[PHONE] = phone
    map[EMAIL] = email
    return map
  }

  private fun createAddApartmentMap(code: String, uid: String, email: String): Map<String, String> {
    val map = HashMap<String, String>()
    map[CODE] = code
    map[UID] = uid
    map[EMAIL] = email
    return map
  }

  private fun createVerifyAdminSecretWord(code: String, uid: String): Map<String, String> {
    val map = HashMap<String, String>()
    map[CODE] = code
    map[UID] = uid
    return map
  }

  private fun createSaveUserUid(uid: String, email: String): Map<String, String> {
    val map = HashMap<String, String>()
    map[UID] = uid
    map[EMAIL] = email
    return map
  }

  private fun createDeleteUserAccount(uid: String, email: String): Map<String, String> {
    val map = HashMap<String, String>()
    map[UID] = uid
    map[EMAIL] = email
    return map
  }
}

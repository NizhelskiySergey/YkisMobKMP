package com.ykis.ykismobkmp.data.apartment


import com.ykis.ykismobkmp.core.Constants.ADDRESS_ID
import com.ykis.ykismobkmp.core.Constants.CODE
import com.ykis.ykismobkmp.core.Constants.EMAIL
import com.ykis.ykismobkmp.core.Constants.PARAM_ADDRESS_ID
import com.ykis.ykismobkmp.core.Constants.PHONE
import com.ykis.ykismobkmp.core.Constants.RAION_ID
import com.ykis.ykismobkmp.core.Constants.UID
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.BaseResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity

// Импорты твоих моделей ответов (GetApartmentsResponse, GetSimpleResponse и т.д.)

/**
 * [ApartmentRemoteImpl] — Реализация удаленного источника данных для KMP.
 * Полностью синхронизирована с типами Long и мапами параметров KtorApiService.
 */
class ApartmentRemoteImpl(
  private val ktorApiService: KtorApiService
) : ApartmentRemote {

  override suspend fun getApartmentList(uid: String): GetApartmentsResponse {
    return ktorApiService.getApartmentList(createGetApartmentListMap(uid))
  }

  override suspend fun getOsbbApartmentsList(targetId: Long, isHouse: Boolean): GetApartmentsResponse {
    // Убрали targetId.toInt(), KtorApiService теперь принимает Long напрямую
    return ktorApiService.getOsbbApartmentsList(
      targetId = targetId,
      map = createGetOsbbApartmentsListMap(targetId, isHouse)
    )
  }

  override suspend fun getHouseByRaionList(raionId: Long): GetHousesResponse {
    // Убрали raionId.toInt(), KtorApiService теперь принимает Long напрямую
    return ktorApiService.getHouseByRaionList(
      raionId = raionId,
      map = createGetHouseByRaionListMap(raionId)
    )
  }

  override suspend fun getRaionList(uid: String): GetRaionsResponse {
    return ktorApiService.getRaionList(createGetRaionListMap(uid))
  }

  override suspend fun updateBti(params: ApartmentEntity): BaseResponse {
    return ktorApiService.updateBti(
      createUpdateBti(
        addressId = params.addressId,
        phone = params.phone,
        email = params.email,
        uid = params.uid ?: ""
      )
    )
  }

  override suspend fun getApartment(addressId: Long, uid: String): GetApartmentResponse {
    // Убрали addressId.toInt(), KtorApiService теперь принимает Long напрямую
    return ktorApiService.getApartment(
      addressId = addressId,
      map = createRequestByAddressId(addressId, uid)
    )
  }

  override suspend fun deleteApartment(addressId: Long, uid: String): BaseResponse {
    // Убрали addressId.toInt(), KtorApiService теперь принимает Long напрямую
    return ktorApiService.deleteApartment(
      addressId = addressId,
      map = createRequestByAddressId(addressId, uid)
    )
  }

  override suspend fun addApartment(code: String, uid: String, email: String): GetSimpleResponse {
    return ktorApiService.addApartmentUser(
      code = code,
      uid = uid,
      map = createAddApartmentMap(code, uid, email)
    )
  }

  override suspend fun verifyAdminSecretWord(code: String, uid: String): GetSimpleResponse {
    return ktorApiService.verifyAdminSecretWord(
      code = code,
      map = createVerifyAdminSecretWord(code, uid)
    )
  }

  override suspend fun saveUserUid(uid: String, email: String): GetSimpleResponse {
    // ИСПРАВЛЕНО: Убран лишний параметр uid, соответствуя сигнатуре KtorApiService
    return ktorApiService.saveUserUid(
      map = createSaveUserUid(uid = uid, email = email)
    )
  }

  override suspend fun deleteUserAccount(uid: String, email: String): GetSimpleResponse {
    // ИСПРАВЛЕНО: Убран лишний параметр uid, соответствуя сигнатуре KtorApiService
    return ktorApiService.deleteUserAccount(
      map = createDeleteUserAccount(uid = uid, email = email)
    )
  }

  // ==========================================
  // СЕРВИСНЫЕ МЕТОДЫ СБОРКИ КРОССПЛАТФОРМЕННЫХ MAP
  // ==========================================

  private fun createGetApartmentListMap(uid: String): Map<String, String> {
    return mapOf(UID to uid)
  }

  private fun createGetRaionListMap(uid: String): Map<String, String> {
    return mapOf(UID to uid)
  }

  private fun createGetOsbbApartmentsListMap(targetId: Long, isHouse: Boolean): Map<String, String> {
    return mapOf(
      "targetId" to targetId.toString(),
      "isHouse" to if (isHouse) "1" else "0"
    )
  }

  private fun createGetHouseByRaionListMap(raionId: Long): Map<String, String> {
    return mapOf(RAION_ID to raionId.toString())
  }

  private fun createRequestByAddressId(addressId: Long, uid: String): Map<String, String> {
    return mapOf(
      PARAM_ADDRESS_ID to addressId.toString(),
      UID to uid
    )
  }

  private fun createUpdateBti(addressId: Long, phone: String, email: String, uid: String): Map<String, String> {
    return mapOf(
      ADDRESS_ID to addressId.toString(),
      PHONE to phone,
      EMAIL to email,
      UID to uid
    )
  }

  private fun createAddApartmentMap(code: String, uid: String, email: String): Map<String, String> {
    return mapOf(
      CODE to code,
      UID to uid,
      EMAIL to email
    )
  }

  private fun createVerifyAdminSecretWord(code: String, uid: String): Map<String, String> {
    return mapOf(
      CODE to code,
      UID to uid
    )
  }

  private fun createSaveUserUid(uid: String, email: String): Map<String, String> {
    return mapOf(
      UID to uid,
      EMAIL to email
    )
  }

  private fun createDeleteUserAccount(uid: String, email: String): Map<String, String> {
    return mapOf(
      UID to uid,
      EMAIL to email
    )
  }
}

package com.ykis.ykismobkmp.data.apartment


import com.ykis.ykismobkmp.data.responses.BaseResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity

// Импорты твоих моделей ответов сервера:
// import com.ykis.ykismobkmp.domain.model.*

/**
 * [ApartmentRemote] — Контракт для работы с удаленным API (Ktor).
 * Все числовые идентификаторы переведены на KMP-совместимый тип Long
 * для бесшовной интеграции с Use Cases и локальной БД SQLDelight.
 */
interface ApartmentRemote {

  suspend fun getApartmentList(uid: String): GetApartmentsResponse

  suspend fun getOsbbApartmentsList(targetId: Long, isHouse: Boolean): GetApartmentsResponse

  suspend fun getRaionList(uid: String): GetRaionsResponse

  suspend fun getHouseByRaionList(raionId: Long): GetHousesResponse

  suspend fun updateBti(params: ApartmentEntity): BaseResponse

  suspend fun getApartment(addressId: Long, uid: String): GetApartmentResponse

  suspend fun deleteApartment(addressId: Long, uid: String): BaseResponse

  suspend fun addApartment(code: String, uid: String, email: String): GetSimpleResponse

  suspend fun verifyAdminSecretWord(code: String, uid: String): GetSimpleResponse

  suspend fun saveUserUid(uid: String, email: String): GetSimpleResponse

  suspend fun deleteUserAccount(uid: String, email: String): GetSimpleResponse
}


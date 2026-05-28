package com.ykis.ykismobkmp.domain.repository.apartment

import com.ykis.ykismobkmp.data.remote.apartment.ApartmentRemote
import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetFamilyResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse

/**
 * [ApartmentRepositoryImpl] — Реализация репозитория, связывающая UseCases с сетевым слоем ApartmentRemote.
 */
class ApartmentRepositoryImpl(
  private val remote: ApartmentRemote
) : ApartmentRepository {

  private val className = "ApartmentRepositoryImpl"

  override suspend fun getApartmentList(uid: String): GetApartmentsResponse {
    println("[$className.getApartmentList]: uid=${uid.takeLast(5)}")
    return try {
      remote.getApartmentList(uid)
    } catch (ex: Exception) {
      println("[$className.getApartmentList] Error: ${ex.message}")
      GetApartmentsResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun updateBti( uid: String,addressId: Long, phone: String, email: String): GetApartmentsResponse {
    println("[$className.updateBti]: uid=$uid,addressId=$addressId, email=${email}, phone=${phone}")
    return try {
      remote.updateBti(uid = uid,addressId = addressId, phone = phone, email = email)
    } catch (ex: Exception) {
      println("[$className.updateBti] Error: ${ex.message}")
      // ИСПРАВЛЕНО НАМЕРТВО: Возвращаем GetApartmentsResponse вместо GetSimpleResponse для строгого соблюдения контракта!
      GetApartmentsResponse(success = 0, message = ex.message ?: "Помилка оновлення БТІ")
    }
  }


  override suspend fun getApartment(uid: String, addressId: Long): GetApartmentResponse {
    println("[$className.getApartment]: addressId=$addressId, uid=${uid.takeLast(5)}")
    return try {
      remote.getApartment(addressId, uid)
    } catch (ex: Exception) {
      println("[$className.getApartment] Error: ${ex.message}")
      GetApartmentResponse(success = 0, message = ex.message ?: "Невідома помилка")
    }
  }

  override suspend fun deleteApartment(uid: String, addressId: Long): GetSimpleResponse {
    println("[$className.deleteApartment]: addressId=$addressId")
    return try {
      val baseResponse = remote.deleteApartment(addressId, uid)
      GetSimpleResponse(success = baseResponse.success, message = baseResponse.message)
    } catch (ex: Exception) {
      println("[$className.deleteApartment] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка видалення квартири")
    }
  }

  override suspend fun addApartmentUser( code: String,uid: String, email: String): GetSimpleResponse {
    println("[$className.addApartmentUser]: code=$code, uid=${uid.takeLast(5)}")
    return try {
      remote.addApartment(code, uid, email)
    } catch (ex: Exception) {
      println("[$className.addApartmentUser] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка додавання користувача")
    }
  }

  override suspend fun verifyAdminSecretWord(uid: String, code: String): GetSimpleResponse {
    println("[$className.verifyAdminSecretWord]: uid=${uid.takeLast(5)}")
    return try {
      remote.verifyAdminSecretWord(code, uid)
    } catch (ex: Exception) {
      println("[$className.verifyAdminSecretWord] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка перевірки коду")
    }
  }

  override suspend fun saveUserUid(uid: String, email: String): GetSimpleResponse {
    println("[$className.saveUserUid]: uid=${uid.takeLast(5)}, email=$email")
    return try {
      remote.saveUserUid(uid, email)
    } catch (ex: Exception) {
      println("[$className.saveUserUid] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка збереження UID")
    }
  }

  override suspend fun deleteUserAccount(uid: String, email: String): GetSimpleResponse {
    println("[$className.deleteUserAccount]: uid=${uid.takeLast(5)}")
    return try {
      remote.deleteUserAccount(uid, email)
    } catch (ex: Exception) {
      println("[$className.deleteUserAccount] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка видалення аккаунту")
    }
  }

  override suspend fun getOsbbApartmentsList(targetId: Long, isHouse: Boolean): GetApartmentsResponse {
    println("[$className.getOsbbApartmentsList]: targetId=$targetId, isHouse=$isHouse")
    return try {
      remote.getOsbbApartmentsList(targetId, isHouse)
    } catch (ex: Exception) {
      println("[$className.getOsbbApartmentsList] Error: ${ex.message}")
      GetApartmentsResponse(success = 0, message = ex.message ?: "Помилка завантаження списку ОСББ")
    }
  }

  override suspend fun getRaionList(uid: String): GetRaionsResponse {
    println("[$className.getRaionList]: uid=${uid.takeLast(5)}")
    return try {
      remote.getRaionList(uid)
    } catch (ex: Exception) {
      println("[$className.getRaionList] Error: ${ex.message}")
      GetRaionsResponse(success = 0, message = ex.message ?: "Помилка завантаження районів")
    }
  }

  override suspend fun getHouseByRaionList(raionId: Long): GetHousesResponse {
    println("[$className.getHouseByRaionList]: raionId=$raionId")
    return try {
      remote.getHouseByRaionList(raionId)
    } catch (ex: Exception) {
      println("[$className.getHouseByRaionList] Сетевой сбой: ${ex.message}")
      GetHousesResponse(
        success = 0,
        message = ex.message ?: "Помилка завантаження будинків",
        houses = emptyList()
      )
    }
  }

  override suspend fun getFamilyList(uid: String, addressId: Long): GetFamilyResponse {
    println("[$className.getFamilyList]: addressId=$addressId, uid=${uid.takeLast(5)}")
    return try {
      remote.getFamilyList(uid, addressId)
    } catch (ex: Exception) {
      println("[$className.getFamilyList] Error: ${ex.message}")
      GetFamilyResponse(
        success = 0,
        message = ex.message ?: "Помилка завантаження складу сім'ї",
        family = emptyList()
      )
    }
  }
}


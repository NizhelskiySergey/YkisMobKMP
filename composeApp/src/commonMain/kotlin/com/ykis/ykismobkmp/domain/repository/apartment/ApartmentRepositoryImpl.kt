package com.ykis.ykismobkmp.domain.repository.apartment

import com.ykis.ykismobkmp.data.remote.apartment.ApartmentRemote
import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetFamilyResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import org.jetbrains.compose.resources.getString
import ykismobkmp.composeapp.generated.resources.*

/**
 * [ApartmentRepositoryImpl] — Реалізація репозиторію, що пов'язує UseCases із мережевим шаром.
 * УНІФІКОВАНО: Всі помилки тепер локалізовані та використовують Res.string.
 */
class ApartmentRepositoryImpl(
  private val remote: ApartmentRemote
) : ApartmentRepository {

  private val className = "ApartmentRepositoryImpl"

  override suspend fun getApartmentList(uid: String): GetApartmentsResponse {
    return try {
      remote.getApartmentList(uid)
    } catch (ex: Exception) {
      println("[$className.getApartmentList] Error: ${ex.message}")
      GetApartmentsResponse(success = 0, message = getString(Res.string.error_network_request_failed))
    }
  }

  override suspend fun updateBti(uid: String, addressId: Long, phone: String, email: String): GetApartmentsResponse {
    return try {
      remote.updateBti(uid = uid, addressId = addressId, phone = phone, email = email)
    } catch (ex: Exception) {
      println("[$className.updateBti] Error: ${ex.message}")
      GetApartmentsResponse(success = 0, message = getString(Res.string.error_update))
    }
  }

  override suspend fun getApartment(uid: String, addressId: Long): GetApartmentResponse {
    return try {
      remote.getApartment(addressId, uid)
    } catch (ex: Exception) {
      println("[$className.getApartment] Error: ${ex.message}")
      GetApartmentResponse(success = 0, message = getString(Res.string.error_unknown))
    }
  }

  override suspend fun deleteApartment(uid: String, addressId: Long): GetSimpleResponse {
    return try {
      val baseResponse = remote.deleteApartment(addressId, uid)
      GetSimpleResponse(success = baseResponse.success, message = baseResponse.message)
    } catch (ex: Exception) {
      println("[$className.deleteApartment] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = getString(Res.string.error_delete_flat))
    }
  }

  override suspend fun addApartmentUser(code: String, uid: String, email: String): GetSimpleResponse {
    return try {
      remote.addApartment(code, uid, email)
    } catch (ex: Exception) {
      println("[$className.addApartmentUser] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = getString(Res.string.error_add_apartment))
    }
  }

  override suspend fun verifyAdminSecretWord(uid: String, code: String): GetSimpleResponse {
    return try {
      remote.verifyAdminSecretWord(code, uid)
    } catch (ex: Exception) {
      println("[$className.verifyAdminSecretWord] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = getString(Res.string.error_incorrect_code))
    }
  }

  override suspend fun saveUserUid(uid: String, email: String): GetSimpleResponse {
    return try {
      remote.saveUserUid(uid, email)
    } catch (ex: Exception) {
      println("[$className.saveUserUid] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = getString(Res.string.error_process))
    }
  }

  override suspend fun deleteUserAccount(uid: String, email: String): GetSimpleResponse {
    return try {
      remote.deleteUserAccount(uid, email)
    } catch (ex: Exception) {
      println("[$className.deleteUserAccount] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = getString(Res.string.error_unknown_deletion))
    }
  }

  override suspend fun getOsbbApartmentsList(targetId: Long, isHouse: Boolean): GetApartmentsResponse {
    return try {
      remote.getOsbbApartmentsList(targetId, isHouse)
    } catch (ex: Exception) {
      println("[$className.getOsbbApartmentsList] Error: ${ex.message}")
      GetApartmentsResponse(success = 0, message = getString(Res.string.error_load_osbb))
    }
  }

  override suspend fun getRaionList(uid: String): GetRaionsResponse {
    return try {
      remote.getRaionList(uid)
    } catch (ex: Exception) {
      println("[$className.getRaionList] Error: ${ex.message}")
      GetRaionsResponse(success = 0, message = getString(Res.string.error_load_raions))
    }
  }

  override suspend fun getHouseByRaionList(raionId: Long): GetHousesResponse {
    return try {
      remote.getHouseByRaionList(raionId)
    } catch (ex: Exception) {
      println("[$className.getHouseByRaionList] Error: ${ex.message}")
      GetHousesResponse(
        success = 0,
        message = getString(Res.string.error_load_houses),
        houses = emptyList()
      )
    }
  }

  override suspend fun getFamilyList(uid: String, addressId: Long): GetFamilyResponse {
    return try {
      remote.getFamilyList(uid, addressId)
    } catch (ex: Exception) {
      println("[$className.getFamilyList] Error: ${ex.message}")
      GetFamilyResponse(
        success = 0,
        message = getString(Res.string.error_load_family),
        family = emptyList()
      )
    }
  }
}

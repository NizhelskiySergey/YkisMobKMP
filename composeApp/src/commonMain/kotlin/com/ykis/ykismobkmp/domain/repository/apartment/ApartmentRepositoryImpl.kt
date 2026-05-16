package com.ykis.ykismobkmp.domain.repository.apartment

import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.BaseResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetFamilyResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity

class ApartmentRepositoryImpl(
  private val apiService: KtorApiService
) : ApartmentRepository {
  private val className = "ApartmentRepositoryImpl"
  override suspend fun getApartmentList(uid: String): GetApartmentsResponse {
    // Используем стандартный кроссплатформенный println для KMP логов
    println("[$className.getApartmentList]: uid=${uid.takeLast(5)}")
    return try {
      val params = mapOf("uid" to uid)
      apiService.getApartmentList(params)
    } catch (ex: Exception) {
      println("[$className.getApartmentList] Error: ${ex.message}")
      GetApartmentsResponse(
        success = 0,
        message = ex.message ?: "Невідома помилка мережі"
      )
    }
  }

  override suspend fun getApartment(uid: String, addressId: Long): GetApartmentResponse {
    println("[$className.getApartment]: addressId=$addressId, uid=${uid.takeLast(5)}")
    return try {
      val params = mapOf("uid" to uid, "address_id" to addressId.toString())
      apiService.getApartment(params)
    } catch (ex: Exception) {
      println("[$className.getApartment] Error: ${ex.message}")
      GetApartmentResponse(
        success = 0,
        message = ex.message ?: "Невідома помилка мережі"
      )
    }
  }

  override suspend fun updateBti(params: ApartmentEntity): GetSimpleResponse {
    println("[$className.updateBti]: addressId=${params.addressId}")

    return try {

      val btiMap = mapOf(
        "address_id" to params.addressId.toString(),
        "fio" to params.fio,
        "phone" to params.phone,
        "email" to params.email
      )
      apiService.updateBti(btiMap)
    } catch (ex: Exception) {
      println("[$className.updateBti] Error: ${ex.message}")
      GetSimpleResponse(
        success = 0,
        message = ex.message ?: "Помилка оновлення БТІ"
      )
    }
  }

  // Изменили тип addressId с Int на Long для соответствия интерфейсу ApartmentRemote
  override suspend fun deleteApartment(uid: String, addressId: Long): GetSimpleResponse {
    println("[$className.deleteApartment]: addressId=$addressId")

    return try {
      // 1. Формируем мапу параметров для PHP-скрипта на лету
      val params = mapOf("uid" to uid, "address_id" to addressId.toString())

      // 2. Передаем в KtorApiService параметры строго по его новой сигнатуре
      apiService.deleteApartment(params)

    } catch (ex: Exception) {
      println("[$className.deleteApartment] Error: ${ex.message}")
      GetSimpleResponse(
        success = 0,
        message = ex.message ?: "Помилка видалення квартири"
      )
    }
  }


  // Имя метода приведено в соответствие с интерфейсом (вероятно addApartment)
  override suspend fun addApartmentUser(
    uid: String,
    code: String,
    email: String
  ): GetSimpleResponse {
    println("[$className.addApartment]: code=$code, uid=${uid.takeLast(5)}")
    return try {
      val params = mapOf(
        "uid" to uid,
        "code" to code,
        "email" to email
      )
      apiService.addApartmentUser(params)
    } catch (ex: Exception) {
      println("[$className.addApartment] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка додавання користувача")
    }
  }
  override suspend fun verifyAdminSecretWord( uid: String,code: String): GetSimpleResponse {
    println("[$className.verifyAdminSecretWord]: uid=${uid.takeLast(5)}")
    return try {
      val params = mapOf(
        "uid" to uid,
        "code" to code
      )
      apiService.verifyAdminSecretWord(params)
    } catch (ex: Exception) {
      println("[$className.verifyAdminSecretWord] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка перевірки коду")
    }
  }
  override suspend fun saveUserUid(uid: String, email: String): GetSimpleResponse {
    println("[$className.saveUserUid]: uid=${uid.takeLast(5)}, email=$email")
    return try {
      val params = mapOf(
        "uid" to uid,
        "email" to email
      )
      apiService.saveUserUid(params)
    } catch (ex: Exception) {
      println("[$className.saveUserUid] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка збереження UID")
    }
  }




  override suspend fun deleteUserAccount(uid: String, email: String): GetSimpleResponse {
    println("[$className.deleteUserAccount]: uid=${uid.takeLast(5)}")
    return try {
      val params = mapOf(
        "uid" to uid,
        "email" to email
      )
      apiService.deleteUserAccount(params)
    } catch (ex: Exception) {
      println("[$className.deleteUserAccount] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка видалення аккаунту")
    }
  }

  override suspend fun getOsbbApartmentsList(
    targetId: Long,
    isHouse: Boolean
  ): GetApartmentsResponse {
    println("[$className.getOsbbApartmentsList]: targetId=$targetId, isHouse=$isHouse")
    return try {
      val params = mapOf(
        "targetId" to targetId.toString(),
        "is_house" to if (isHouse) "1" else "0"
      )
      // KtorApiService принимает Long напрямую
      apiService.getOsbbApartmentsList( params)
    } catch (ex: Exception) {
      println("[$className.getOsbbApartmentsList] Error: ${ex.message}")
      GetApartmentsResponse(success = 0, message = ex.message ?: "Помилка завантаження списку ОСББ")
    }
  }

  override suspend fun getRaionList(uid: String): GetRaionsResponse {
    println("[$className.getRaionList]: uid=${uid.takeLast(5)}")
    return try {
      val params = mapOf("uid" to uid)
      apiService.getRaionList(params)
    } catch (ex: Exception) {
      println("[$className.getRaionList] Error: ${ex.message}")
      GetRaionsResponse(success = 0, message = ex.message ?: "Помилка завантаження районів")
    }
  }

  override suspend fun getHouseByRaionList(raionId: Long): GetHousesResponse {
    println("[$className.getHouseByRaionList]: raionId=$raionId")

    return try {
      // 1. Формируем мапу параметров для PHP-скрипта на сервере
      val params = mapOf("raion_id" to raionId.toString())

      // 2. Делаем чистый вызов KtorApiService
      apiService.getHouseByRaionList(params)

    } catch (ex: Exception) {
      println("[$className.getHouseByRaionList] Сетевой сбой: ${ex.message}")
      // Гасим краш и возвращаем объект ошибки, чтобы UseCase ушел в локальный кэш
      GetHousesResponse(
        success = 0,
        message = ex.message ?: "Помилка завантаження будинків",
        houses = emptyList()
      )
    }
  }
  override suspend fun getFamilyList(uid: String,addressId: Long): GetFamilyResponse {
    println("[$className.getFamilyList]: addressId=$addressId")

    return try {
      // 1. Формируем мапу параметров для PHP-скрипта на сервере
      val params = mapOf("uid" to uid, "address_id" to addressId.toString())

      // 2. Делаем чистый вызов KtorApiService
      apiService.getFamilyList( params)

    } catch (ex: Exception) {
      println("[$className.getFamilyList] Критическая ошибка сети: ${ex.message}")
      // Безопасно возвращаем объект ответа с зашитой ошибкой вместо падения приложения
      GetFamilyResponse(
        success = 0,
        message = ex.message ?: "Помилка зв'язку з сервером",
        family = emptyList()
      )
    }
  }


}


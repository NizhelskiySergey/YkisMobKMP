package com.ykis.ykismobkmp.domain.repository.apartment

import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.BaseResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
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
      // РЕШЕНИЕ: Создаем мапу параметров на лету с помощью mapOf()
      // и передаем ее в KtorApiService, который именно этого и ожидает
      apiService.getApartmentList(mapOf("uid" to uid))
    } catch (ex: Exception) {
      println("[$className.getApartmentList] Error: ${ex.message}")
      GetApartmentsResponse(
        success = 0,
        message = ex.message ?: "Невідома помилка мережі"
      )
    }
  }


  override suspend fun getApartment(addressId: Long, uid: String): GetApartmentResponse {
    println("[$className.getApartment]: addressId=$addressId, uid=${uid.takeLast(5)}")

    return try {
      // 1. Формируем мапу параметров для PHP-скрипта на сервере
      val paramsMap = mapOf(
        "address_id" to addressId.toString(),
        "uid" to uid
      )

      // 2. Передаем в KtorApiService строго по его новой сигнатуре (Long + Map)
      apiService.getApartment(addressId = addressId, map = paramsMap)
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
      // РЕШЕНИЕ: Собираем мапу из полей ApartmentEntity на лету
      val btiMap = mapOf(
        "address_id" to params.addressId.toString(),
        "fio" to params.fio,
        "phone" to params.phone,
        "email" to params.email
      )

      // Передаем мапу в KtorApiService, как он и ожидает
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
  override suspend fun deleteApartment(addressId: Long, uid: String): GetSimpleResponse {
    println("[$className.deleteApartment]: addressId=$addressId")

    return try {
      // 1. Формируем мапу параметров для PHP-скрипта на лету
      val paramsMap = mapOf(
        "address_id" to addressId.toString(),
        "uid" to uid
      )

      // 2. Передаем в KtorApiService параметры строго по его новой сигнатуре
      apiService.deleteApartment(addressId = addressId, map = paramsMap)

    } catch (ex: Exception) {
      println("[$className.deleteApartment] Error: ${ex.message}")
      GetSimpleResponse(
        success = 0,
        message = ex.message ?: "Помилка видалення квартири"
      )
    }
  }


  // Имя метода приведено в соответствие с интерфейсом (вероятно addApartment)
  override suspend fun addApartmentUser(code: String, uid: String, email: String): GetSimpleResponse {
    println("[$className.addApartment]: code=$code, uid=${uid.takeLast(5)}")
    return try {
      val paramsMap = mapOf(
        "code" to code,
        "uid" to uid,
        "email" to email
      )
      apiService.addApartmentUser(code = code, uid = uid, map = paramsMap)
    } catch (ex: Exception) {
      println("[$className.addApartment] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка додавання користувача")
    }
  }

  override suspend fun verifyAdminSecretWord(code: String, uid: String): GetSimpleResponse {
    println("[$className.verifyAdminSecretWord]: uid=${uid.takeLast(5)}")
    return try {
      val paramsMap = mapOf(
        "code" to code,
        "uid" to uid
      )
      apiService.verifyAdminSecretWord(code = code, map = paramsMap)
    } catch (ex: Exception) {
      println("[$className.verifyAdminSecretWord] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка перевірки коду")
    }
  }

  override suspend fun saveUserUid(uid: String, email: String): GetSimpleResponse {
    println("[$className.saveUserUid]: uid=${uid.takeLast(5)}, email=$email")
    return try {
      val paramsMap = mapOf(
        "uid" to uid,
        "email" to email
      )
      apiService.saveUserUid(map = paramsMap)
    } catch (ex: Exception) {
      println("[$className.saveUserUid] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка збереження UID")
    }
  }

  override suspend fun deleteUserAccount(uid: String, email: String): GetSimpleResponse {
    println("[$className.deleteUserAccount]: uid=${uid.takeLast(5)}")
    return try {
      val paramsMap = mapOf(
        "uid" to uid,
        "email" to email
      )
      apiService.deleteUserAccount(map = paramsMap)
    } catch (ex: Exception) {
      println("[$className.deleteUserAccount] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Помилка видалення аккаунту")
    }
  }

  override suspend fun getOsbbApartmentsList(targetId: Long, isHouse: Boolean): GetApartmentsResponse {
    println("[$className.getOsbbApartmentsList]: targetId=$targetId, isHouse=$isHouse")
    return try {
      val paramsMap = mapOf(
        "target_id" to targetId.toString(),
        "is_house" to if (isHouse) "1" else "0"
      )
      // KtorApiService принимает Long напрямую
      apiService.getOsbbApartmentsList(targetId = targetId, map = paramsMap)
    } catch (ex: Exception) {
      println("[$className.getOsbbApartmentsList] Error: ${ex.message}")
      GetApartmentsResponse(success = 0, message = ex.message ?: "Помилка завантаження списку ОСББ")
    }
  }

  override suspend fun getRaionList(uid: String): GetRaionsResponse {
    println("[$className.getRaionList]: uid=${uid.takeLast(5)}")
    return try {
      val paramsMap = mapOf("uid" to uid)
      apiService.getRaionList(map = paramsMap)
    } catch (ex: Exception) {
      println("[$className.getRaionList] Error: ${ex.message}")
      GetRaionsResponse(success = 0, message = ex.message ?: "Помилка завантаження районів")
    }
  }

  // [ApartmentRemoteImpl.kt]

  /**
   * [ApartmentRemoteImpl.getHouseByRaionList] — Получение списка домов по эталону квартир.
   * Убраны ручные диспетчеры, управление потоками на 100% делегировано движку Ktor.
   */
  override suspend fun getHouseByRaionList(raionId: Long): GetHousesResponse {
    println("[$className.getHouseByRaionList]: raionId=$raionId")

    return try {
      // 1. Формируем мапу параметров для PHP-скрипта на сервере
      val paramsMap = mapOf("raion_id" to raionId.toString())

      // 2. Делаем чистый вызов KtorApiService
      apiService.getHouseByRaionList(raionId = raionId, map = paramsMap)

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


}


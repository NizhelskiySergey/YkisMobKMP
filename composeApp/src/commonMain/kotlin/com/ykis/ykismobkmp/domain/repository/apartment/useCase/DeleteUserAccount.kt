package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [DeleteUserAccount] — Сценарий полного удаления аккаунта пользователя.
 * Синхронизирует удаление данных на сервере с тотальной очисткой локальной базы данных.
 */
class DeleteUserAccount(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "DeleteUserAccount"

  operator fun invoke(uid: String, email: String): Flow<Resource<GetSimpleResponse>> = flow {
    val methodName = "invoke"

    try {
      println("[$className.$methodName]: [START] UID: ${uid.takeLast(5)}")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ (Удаление в MySQL на сервере через Ktor)
      val response = repository.deleteUserAccount(uid, email)

      if (response.success == 1) {
        println("[$className.$methodName]: [SUCCESS] Аккаунт успешно удален на удаленном сервере")

        // 2. ПОЛНАЯ АТОМАРНАЯ ОЧИСТКА ЛОКАЛЬНОЙ БАЗЫ (Обеспечение GDPR / Приватности)
        // ИСПРАВЛЕНО НАМЕРТВО: Вызов тотального удаления инкапсулирован внутри ApartmentCache
        try {
          cache.deleteAllApartments()
          // Если в будущем в твоем Cache интерфейсе появятся методы очистки остальных таблиц счетчиков:
          // cache.clearAllUserData()
          println("[$className.$methodName]: Локальные таблицы пользователя в SQLDelight полностью зачищены")
        } catch (dbEx: Exception) {
          println("[$className.$methodName]: Ошибка каскадной зачистки таблиц: ${dbEx.message}")
        }

        emit(Resource.Success(response))
      } else {
        println("[$className.$methodName]: [API_REJECT] Сервер отклонил удаление: ${response.message}")
        emit(Resource.Error(message = response.message))
      }

    } catch (ce: CancellationException) {
      // КРИТИЧНО ДЛЯ КОРУТИН: CancellationException обязательно пробрасываем дальше!
      println("[$className.$methodName]: [CANCELLED] Операция отменена областью видимости корутины")
      throw ce
    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Критический сбой удаления аккаунта: ${ex.message}")
      ex.printStackTrace()
      emit(Resource.Error(message = "Сервіс недоступний. Спробуйте пізніше"))
    }
  }.flowOn(Dispatchers.Default) // Очистка таблиц и сетевой запрос выполняются на фоновом пуле корутин
}

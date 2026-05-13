package com.ykis.ykismobkmp.domain.repository.apartment.usecase


import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.db.YkisDatabases
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [DeleteUserAccount] — Сценарий полного удаления аккаунта пользователя.
 * Синхронизирует удаление данных на MySQL сервере с тотальной очисткой кэша SQLDelight на Mac/Android.
 */
class DeleteUserAccount(
  private val repository: ApartmentRepository,
  private val database: YkisDatabases // Инжектируем актуальный кроссплатформенный класс базы
) {
  operator fun invoke(uid: String, email: String): Flow<Resource<GetSimpleResponse>> = flow {
    val methodName = "UseCase.DeleteUserAccount"

    try {
      println("[$methodName]: [START] UID: ${uid.takeLast(5)}")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ (Удаление в MySQL на сервере через Ktor)
      val response = repository.deleteUserAccount(uid, email)

      if (response.success == 1) {
        println("[$methodName]: [SUCCESS] Аккаунт успешно удален на удаленном сервере")

        // 2. ПОЛНАЯ АТОМАРНАЯ ОЧИСТКА ЛОКАЛЬНОЙ БАЗЫ (Обеспечение GDPR / Приватности)
        // ИСПРАВЛЕНО: Все запросы теперь вызываются из единого ykisDatabasesQueries
        database.ykisDatabasesQueries.transaction {
          database.ykisDatabasesQueries.deleteAllApartments()
          database.ykisDatabasesQueries.deleteAllWaterMeters()
          database.ykisDatabasesQueries.deleteAllHeatMeters()
          database.ykisDatabasesQueries.deleteAllPayments()
          database.ykisDatabasesQueries.deleteAllRaions()
          database.ykisDatabasesQueries.deleteAllHouses()
          database.ykisDatabasesQueries.deleteAllService() // Добавили очистку начислений ЖКХ
        }
        println("[$methodName]: Локальный файл БД SQLDelight полностью зачищен")

        emit(Resource.Success(response))
      } else {
        println("[$methodName]: [API_REJECT] ${response.message}")
        emit(Resource.Error(message = response.message ?: "Помилка видалення профілю"))
      }

    } catch (ce: CancellationException) {
      // КРИТИЧНО ДЛЯ КОРУТИН: CancellationException нельзя гасить в catch(e: Exception),
      // его обязательно пробрасываем дальше, чтобы область видимости (Scope) завершилась корректно.
      println("[$methodName]: [CANCELLED] Операция отменена корутиной")
      throw ce
    } catch (ex: Exception) {
      println("[$methodName]: [FATAL_ERROR] ${ex.message}")
      emit(Resource.Error(message = "Сервіс недоступний. Спробуйте пізніше"))
    }
  }.flowOn(Dispatchers.Default) // Очистка всех таблиц — дисковая IO-операция, выполняем на фоновом пуле
}



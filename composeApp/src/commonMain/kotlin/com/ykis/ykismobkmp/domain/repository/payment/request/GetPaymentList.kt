package com.ykis.ykismobkmp.domain.repository.payment.request

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.repository.payment.PaymentRepository
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val tag = "UseCase.GetPaymentList"

/**
 * [GetPaymentList] — Доменный Use Case для загрузки и локального кэширования архива оплат абонента ГИОЦ Южного.
 * Полностью автономен, изолирован от баз данных через функциональные КМР-лямбды и готов к сборке на Mac Desktop.
 */
class GetPaymentList(
  private val repository: PaymentRepository,
  // Настраиваем лямбды работы с локальным кэшем SQLDelight через сквозной тип Long
  private val getLocal: suspend (Long) -> List<PaymentEntity> = { emptyList() },
  private val saveLocal: suspend (Long, List<PaymentEntity>) -> Unit = { _, _ -> }
) {
  /**
   * [invoke] — Выполнение Use Case.
   * ИСПРАВЛЕНО: addressId переведен из Int на Long под КМР-стандарт СУБД.
   */
  operator fun invoke(addressId: Long, year: String, uid: String): Flow<Resource<List<PaymentEntity>>> = flow {
    val methodName = "invoke"
    try {
      println("[$tag.$methodName]: [START] AddrID: $addressId, Year: $year")
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Вызов КМР-лямбды)
      val localPayments = getLocal(addressId)
      if (localPayments.isNotEmpty()) {
        println("[$tag.$methodName]: [DB_HIT] Знайдено в базі: ${localPayments.size}")
        emit(Resource.Success(localPayments))
      } else {
        println("[$tag.$methodName]: [DB_MISS] Локальна база порожня")
      }

      // 2. ЗАПРОС В СЕТЬ (Через Ktor репозиторий напрямую)
      println("[$tag.$methodName]: [NETWORK_REQ] Відправка запиту до сервера оплат...")
      val response = repository.getPaymentList(uid,addressId, year )

      if (response.success == 1) {
        val remotePayments = response.payments ?: emptyList()
        println("[$tag.$methodName]: [NETWORK_SUCCESS] Отримано з мережі: ${remotePayments.size}")

        // Перезаписываем локальный кэш через лямбду
        saveLocal(addressId, remotePayments)

        // Отдаем финальный актуальный список в UI
        emit(Resource.Success(remotePayments))
      } else {
        println("[$tag.$methodName]: [SERVER_REJECT] Success=0, Message: ${response.message}")
        if (localPayments.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Помилка завантаження архіву оплат"))
        }
      }

    } catch (e: ResponseException) {
      println("[$tag.$methodName]: [HTTP_ERROR] ${e.response.status}")
      SnackbarManager.showMessage(e.response.status.description)
      emit(Resource.Error())
    } catch (ex: Exception) {
      println("[$tag.$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")

      // OFFLINE RECOVERY: При любой ошибке сети/сервера пробуем еще раз отдать локальный кэш
      val lastHope = getLocal(addressId)
      if (lastHope.isNotEmpty()) {
        println("[$tag.$methodName]: [OFFLINE_MODE] Показ даних з кэшу при збої мережі")
        emit(Resource.Success(lastHope))
      } else {
        SnackbarManager.showMessage("Відсутній зв'язок з сервером ГІОЦ")
        emit(Resource.Error(message = "Відсутній зв'язок та немає збережених даних"))
      }
    }
  }.flowOn(Dispatchers.Default) // ИСПРАВЛЕНО: Безопасный КМР-пул потоков вместо Dispatchers.IO
}




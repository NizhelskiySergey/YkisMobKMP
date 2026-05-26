package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetApartmentList(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "GetApartmentList"

  operator fun invoke(uid: String): Flow<Resource<List<ApartmentEntity>>> = flow {
    val methodName = "invoke"

    if (uid.isBlank()) {
      println("[YkisLogKMP.$className.$methodName]: [ABORT] Передано порожній ідентифікатор користувача UID")
      emit(Resource.Error("Помилка авторизації"))
      return@flow
    }

    var localList = emptyList<ApartmentEntity>()

    try {
      // ЕТАП 1: Виставляємо стейт завантаження для UI
      emit(Resource.Loading())

      // ЕТАП 2: ШВИДКИЙ СТАРТ — Витягуємо з SQLDelight кЕш та фільтруємо по UID користувача
      localList = cache.getApartmentsByUser().filter { it.uid == uid }
      if (localList.isNotEmpty()) {
        println("[YkisLogKMP.$className.$methodName]: [LOCAL_HIT] Виведено ${localList.size} квартир з локального кЕшу")
        emit(Resource.Success(localList))
      }

      // ЕТАП 3: ОБНОВЛЕННЯ — Йдемо через Ktor HTTP-клієнт на видалений сервер ЮКІС
      println("[YkisLogKMP.$className.$methodName]: [NETWORK_START] Запит свіжих даних для UID: ${uid.takeLast(5)}")

      val response: GetApartmentsResponse = try {
        repository.getApartmentList(uid)
      } catch (parseException: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [PARSER_WARN] БЕКЕНД ПОВЕРНУВ НЕВАЛІДНИЙ JSON (text/html або порожнеча). Спрацював локальний запобіжник. Error: ${parseException.message}")
        // Створюємо штучну успішну відповідь з порожнім списком, щоб обійти NoTransformationFoundException
        GetApartmentsResponse(success = 1, apartments = emptyList(), message = "Empty fallback by error")
      }

      if (response.success == 1) {
        val remoteApartments = response.apartments ?: emptyList()

        // Прошиваємо UID користувача для кожної отриманої квартири
        val apartmentsWithUid = remoteApartments.map { it.copy(uid = uid) }

        // ЕТАП 4: АТОМАРНА СИНХРОНІЗАЦІЯ — Перезаписуємо локальну БД SQLDelight 2.x
        cache.deleteAllApartments()
        cache.insertApartmentList(apartmentsWithUid)

        println("[YKISLOGKMP.$className.$methodName]: [SYNC_SUCCESS] КЕш СУБД успішно оновлено (${apartmentsWithUid.size} кв.)")
        emit(Resource.Success(apartmentsWithUid))
      } else {
        println("[YkisLogKMP.$className.$methodName]: [NETWORK_REJECT] Сервер повернув статус помилки: ${response.message}")

        // КРИТИЧЕСКИЙ ФИКС ДЛЯ НОВЫХ ПОЛЬЗОВАТЕЛЕЙ БЕЗ КВАРТИР:
        // Якщо сервер відповів відмовою (або помилкою парсера), але це абсолютно новий користувач (кЕш порожній) —
        // примусово емітуємо Success(emptyList()) замість ломаючого Resource.Error. Це чисто гасить лоадер в UI!
        if (localList.isEmpty()) {
          println("[YkisLogKMP.$className.$methodName]: [FALLBACK_REJECT] Новий користувач. Емітуємо порожній Success список для розблокування UI.")
          emit(Resource.Success(emptyList()))
        } else {
          emit(Resource.Success(localList))
        }
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.$className.$methodName]: [FATAL_ERROR] Каскадний збій мережі або десеріалізації Ktor: ${ex.message}")
      ex.printStackTrace()

      // КРИТИЧЕСКИЙ ФИКС В ГЛОБАЛЬНОМ CATCH БЛОКЕ:
      // Гарантуємо, що якщо корутина впала через NoTransformationFoundException, новий абонент не зависне на лоадері!
      if (localList.isEmpty()) {
        println("[YkisLogKMP.$className.$methodName]: [FALLBACK_CATCH] Новий користувач. Емітуємо порожній Success список при критичному збої.")
        emit(Resource.Success(emptyList()))
      } else {
        // ЕТАП 5: OFFLINE MODE — Якщо інтернету немає, але кЕш був — піднімаємо локальний архив
        println("[YkisLogKMP.$className.$methodName]: [OFFLINE_MODE] Мережа недоступна, додаток переведено на локальний архів")
        emit(Resource.Success(localList))
      }
    }
  }.flowOn(Dispatchers.Default)
}

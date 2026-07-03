package com.ykis.ykismobkmp.domain.services


/**
 * [ClearDatabase] — Автономный Use Case каскадного удаления локального кэша ЖКХ-фонда.
 */

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.mp.KoinPlatform

/**
 * [ClearDatabase] — Доменный Use Case каскадного удаления локального кэша ЖКХ-фонда.
 * ИСПРАВЛЕНО НАМЕРТВО: Конструктор полностью очищен от параметров.
 * Таблицы SQLDelight вычитываются напрямую через KoinPlatform в момент вызова,
 * что на 100% страхует холодный старт приложения от InstanceCreationException!
 */
class ClearDatabase {

  operator fun invoke(): Flow<Resource<Unit>> = flow {
    try {
      // РЕШЕНИЕ: Извлекаем таблицы запросов напрямую из рантайма в обход резолвера CoreResolverV2!
      val queries = KoinPlatform.getKoin().get<YkisDatabasesQueries>()

      queries.transaction {
        queries.deleteAllApartments()
        queries.deleteAllFamily()
        queries.deleteAllHeatReadings()
        queries.deleteAllHeatMeters()
        queries.deleteAllWaterMeters()
        queries.deleteAllWaterReadings()
      }
      emit(Resource.Success(Unit))
    } catch (e: Exception) {
      println("[ClearDatabase.invoke]: [ERROR] Сбой транзакции очистки СУБД: ${e.message}")
      emit(Resource.Error(e.message ?: "Помилка очищення БД"))
    }
  }
}


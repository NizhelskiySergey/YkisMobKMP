package com.ykis.ykismobkmp.ui.screens.family

import cafe.adriel.voyager.core.model.screenModelScope // Кроссплатформенный Scope Voyager
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val tag = "FamilyListScreenModel"

// Кроссплатформенное реактивное состояние экрана состава семьи
data class FamilyState(
  val familyList: List<FamilyEntity> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null
)

// КМР-параметры вызова Use Case
data class FamilyParams(
  val uid: String,
  val addressId: Long // ИСПРАВЛЕНО: Сквозной КМР-тип Long под SQLDelight
)

// Заглушка интерактора, если он импортируется из Use Case слоя
typealias GetFamilyList = (FamilyParams) -> Flow<Resource<List<FamilyEntity>>>

/**
 * [FamilyListScreenModel] — Кроссплатформенная модель управления списками зарегистрированных жильцов ЮКИС.
 * Полностью типизирована под Long-идентификаторы и готова к выполнению на Mac Desktop (JVM) и iOS.
 */
class FamilyListScreenModel(
  private val getFamilyListUseCase: GetFamilyList,
  logService: LogService
) : BaseScreenModel(logService) {

  private val _state = MutableStateFlow(FamilyState())
  val state: StateFlow<FamilyState> = _state.asStateFlow()

  /**
   * [getFamilyList] — Каскадный асинхронный сбор зарегистрированных мешканців квартиры из биллинга г. Южный.
   * ИСПРАВЛЕНО: addressId переведен из Int на Long под КМР-стандарт СУБД.
   */
  fun getFamilyList(uid: String, addressId: Long) {
    val methodName = "getFamilyList"
    if (addressId <= 0L) return

    println("[$tag.$methodName]: [START] Запрос состава семьи для о/р: $addressId")

    // Вызываем КМР Use Case, передавая строго типизированные Long-параметры
    getFamilyListUseCase(FamilyParams(uid, addressId)).onEach { result ->
      when (result) {
        is Resource.Success -> {
          println("[$tag.$methodName]: [SUCCESS] Из биллинга получено ${result.data?.size} жильцов")
          _state.value = FamilyState(
            familyList = result.data ?: emptyList(),
            isLoading = false
          )
        }
        is Resource.Error -> {
          println("[$tag.$methodName]: [ERROR] Сбой загрузки: ${result.message}")
          _state.value = FamilyState(
            error = result.message ?: "Помилка завантаження даних"
          )
        }
        is Resource.Loading -> {
          _state.value = FamilyState(isLoading = true)
        }
      }
    }.launchIn(screenModelScope) // ИСПРАВЛЕНО: Корутина привязана к КМР жизненному циклу Voyager
  }
}

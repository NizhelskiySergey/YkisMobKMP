package com.ykis.ykismobkmp.ui.screens.appartment

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetFamilyList
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import kotlinx.coroutines.flow.update

class FamilyListScreenModel(
  private val getFamilyListUseCase: GetFamilyList,
  logService: LogService
) : BaseScreenModel(logService) {

  private val className = "FamilyListScreenModel"

  /**
   * [getFamilyList] — Каскадный асинхронный сбор зарегистрированных мешканців квартиры из биллинга г. Южный.
   */
  fun getFamilyList(uid: String, addressId: Long) {
    val methodName = "getFamilyList"
    if (addressId <= 0L) return

    launchCatching(showLoader = true) {
      println("[YkisLogKMP.$className.$methodName]: [START] Запрос состава семьи для о/р: $addressId")

      getFamilyListUseCase(uid, addressId).collect { result ->
        _uiState.update { currentState ->
          when (result) {
            is Resource.Success -> {
              println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Загружено ${result.data?.size} чел.")
              currentState.copy(
                familyList = result.data ?: emptyList(),
                mainLoading = false
              )
            }
            is Resource.Error -> {
              println("[YkisLogKMP.$className.$methodName]: [ERROR] ${result.message}")
              currentState.copy(
                mainLoading = false,
                error = result.message
              )
            }
            is Resource.Loading -> {
              currentState.copy(mainLoading = true)
            }
          }
        }
      }
    }
  }
}

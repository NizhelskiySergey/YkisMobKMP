package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.BaseResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class UpdateBti(
    private val repository: ApartmentRepository,
) {
  operator fun invoke(params: ApartmentEntity): Flow<Resource<GetSimpleResponse>> = flow {
      val methodName = "UseCase.UpdateBti"
      try {
          Log.d("YkisLog", "[$methodName]: [START] ID: ${params.addressId}")
          emit(Resource.Loading())

          // 1. Валидация ПЕРЕД запросом (Тот самый принцип "якоря")
          if (params.address.isBlank()) {
              emit(Resource.Error(message = "Адреса не может быть пустой"))
              return@flow
          }

          // 2. Сетевой запрос через мультиплатформенный репозиторий
          val response = repository.updateBti(params)

          if (response.success == 1) {
              Log.i("YkisLog", "[$methodName]: [SUCCESS]")
              emit(Resource.Success(response))
          } else {
              Log.e("YkisLog", "[$methodName]: [REJECT] ${response.message}")
              // В KMP можно передавать либо текст, либо ключ ресурса (errorKey)
              emit(Resource.Error(message = response.message))
          }

      } catch (ex: Exception) {
          Log.e("YkisLog", "[$methodName]: [EXCEPTION] ${ex.message}")
          // В Wasm/JS IOException нет, ловим общий Exception
          emit(Resource.Error(message = "Помилка мережі або сервера"))
      }
  }.flowOn(Dispatchers.Default) // В KMP Default работает везде, IO — не на всех таргетах
}

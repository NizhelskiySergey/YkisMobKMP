package com.ykis.ykismobkmp.data.family

import com.ykis.ykismobkmp.core.Constants.ADDRESS_ID
import com.ykis.ykismobkmp.core.Constants.UID
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.domain.entity.FamilyEntity


class FamilyRemoteImpl (
    private val ktorApiService: KtorApiService
) : FamilyRemote {


    private fun createGetFamilyListMap(
        addressId: Int,
        uid: String
    ): Map<String, String> {
        val map = HashMap<String, String>()
        map[ADDRESS_ID] = addressId.toString()
        map[UID] = uid
        return map
    }


  override suspend fun getFamilyList(params: FamilyParams): List<FamilyEntity> {
    // 1. Вызываем метод (он уже suspend и возвращает GetFamilyResponse)
    val response = ktorApiService.getFamilyList(
      createGetFamilyListMap(
        addressId = params.addressId,
        uid = params.uid
      )
    )

    // 2. Возвращаем список из ответа (убрали .await())
    return response.family ?: emptyList()
  }


}

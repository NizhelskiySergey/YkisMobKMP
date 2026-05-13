package com.ykis.ykismobkmp.data.family

import com.ykis.ykismobkmp.domain.entity.FamilyEntity

data class FamilyParams(
  val uid : String,
  val addressId : Int
)
interface FamilyRemote {
    suspend fun getFamilyList(
        params:FamilyParams
    ):List<FamilyEntity>
}

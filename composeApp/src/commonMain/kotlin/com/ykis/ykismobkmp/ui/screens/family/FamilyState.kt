package com.ykis.ykismobkmp.ui.screens.family

import com.ykis.mob.domain.family.FamilyEntity

data class FamilyState(
    val familyList: List<FamilyEntity> = emptyList(),
    val isLoading:Boolean = false,
    val error : String = ""
)

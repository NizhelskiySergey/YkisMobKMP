package com.ykis.ykismobkmp.ui.screens.service.detail

import com.ykis.mob.domain.service.ServiceEntity

data class ServiceState(
    val services: List<ServiceEntity> = emptyList(),
    val error : String = "",
    val isLoading:Boolean = true
)

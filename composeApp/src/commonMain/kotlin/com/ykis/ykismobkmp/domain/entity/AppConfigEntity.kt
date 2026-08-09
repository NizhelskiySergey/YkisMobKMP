package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateConfig(
    @SerialName("latestVersion") val latestVersion: String = "",
    @SerialName("androidUrl") val androidUrl: String = "",
    @SerialName("iosUrl") val iosUrl: String = "",
    @SerialName("webUrl") val webUrl: String = "",
    @SerialName("isCritical") val isCritical: Boolean = false
)

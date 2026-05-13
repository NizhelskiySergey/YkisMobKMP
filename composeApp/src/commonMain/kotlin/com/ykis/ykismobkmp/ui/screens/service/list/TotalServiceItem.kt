package com.ykis.ykismobkmp.ui.screens.service.list

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ykis.mob.ui.navigation.ContentDetail

data class TotalServiceDebt(
    val name : String,
    val contentDetail: ContentDetail,
    val icon : ImageVector,
    val color : Color,
    val debt : Double,
)

package com.ykis.ykismobkmp.ui.screens.auth

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    val phoneNumber: String = "",
    val smsCode: String = "",
    val isSmsSent: Boolean = false
)

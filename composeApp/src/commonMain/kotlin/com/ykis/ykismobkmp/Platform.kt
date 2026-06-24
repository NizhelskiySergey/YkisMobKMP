package com.ykis.ykismobkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/**
 * [restartApp] — Примусове перезавантаження додатку для застосування налаштувань (наприклад, мови).
 */
expect fun restartApp()

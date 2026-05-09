package com.ykis.ykismobkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
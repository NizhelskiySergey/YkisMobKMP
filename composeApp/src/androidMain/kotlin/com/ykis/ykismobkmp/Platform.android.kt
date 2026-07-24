package com.ykis.ykismobkmp

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val appVersion: String = AppConfig.APP_VERSION
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun restartApp() {
    // На Android перезапуск зазвичай не потрібен для зміни локалі Compose
    println("[AndroidPlatform]: restartApp called (No-op placeholder)")
}

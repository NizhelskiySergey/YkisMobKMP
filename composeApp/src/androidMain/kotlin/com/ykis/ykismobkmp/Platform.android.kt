package com.ykis.ykismobkmp

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun restartApp() {
    // На Android мова зазвичай змінюється через ресурси системи,
    // але якщо потрібно програмно - Activity.recreate()
}

package com.ykis.ykismobkmp

import kotlinx.browser.window

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
    override val appVersion: String = AppConfig.APP_VERSION
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun restartApp() {
    window.location.reload()
}

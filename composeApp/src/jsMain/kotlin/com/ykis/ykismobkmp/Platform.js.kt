package com.ykis.ykismobkmp

import kotlinx.browser.window

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
    override val appVersion: String = AppConfig.APP_VERSION
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun restartApp() {
    val currentUrl = window.location.href.substringBefore("?")
    val timestamp = kotlin.js.Date().getTime()
    window.location.assign("$currentUrl?v=$timestamp")
}

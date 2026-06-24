package com.ykis.ykismobkmp

import kotlinx.browser.window

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun restartApp() {
    window.location.reload()
}

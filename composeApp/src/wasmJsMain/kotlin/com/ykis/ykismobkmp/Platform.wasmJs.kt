package com.ykis.ykismobkmp

import kotlinx.browser.window

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun restartApp() {
    window.location.reload()
}

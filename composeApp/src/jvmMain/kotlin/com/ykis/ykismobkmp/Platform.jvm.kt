package com.ykis.ykismobkmp

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val appVersion: String = AppConfig.APP_VERSION
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun restartApp() {
    println("[JVMPlatform]: restartApp called (No-op placeholder)")
}

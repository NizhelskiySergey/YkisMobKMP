package com.ykis.ykismobkmp

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun restartApp() {
    // На iOS програмне перезавантаження не рекомендується Apple
}

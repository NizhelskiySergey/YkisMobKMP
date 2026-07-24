package com.ykis.ykismobkmp

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    
    override val appVersion: String
        get() {
            val bundle = NSBundle.mainBundle
            return bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0.0"
        }
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun restartApp() {
    // На iOS програмне перезавантаження не рекомендується Apple
}

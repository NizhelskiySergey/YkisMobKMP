package com.ykis.ykismobkmp.core.utils
import platform.UIKit.UIApplication
import platform.UIKit.UIUserNotificationSettings
import platform.UIKit.UIUserNotificationTypeBadge

actual fun applyAppBadgeCount(count: Int) {
  // В iOS для установки баджа нужно использовать нативный UIApplication
  val app = UIApplication.sharedApplication

  // В новых версиях iOS бадж устанавливается через свойство applicationIconBadgeNumber
  app.applicationIconBadgeNumber = count.toLong()
}

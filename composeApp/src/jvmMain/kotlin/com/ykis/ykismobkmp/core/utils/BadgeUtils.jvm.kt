package com.ykis.ykismobkmp.core.utils

import java.awt.Taskbar
import java.awt.Desktop

actual fun applyAppBadgeCount(count: Int) {
  // Проверяем, поддерживает ли текущая ОС (Mac) работу с таскбаром
  try {
    if (!Desktop.isDesktopSupported()) return

    val taskbar = Taskbar.getTaskbar()

    // iconBadgeNumber принимает строку или null для сброса
    val label = if (count > 0) count.toString() else null

    // Устанавливаем бадж (работает на macOS Monterey и новее)
    taskbar.setIconBadge(label)

  } catch (e: UnsupportedOperationException) {
    // Эта ошибка вылетит на Windows/Linux, где баджи не поддерживаются так, как на Mac
    println("[BadgeUtils]: Badges not supported on this OS")
  } catch (e: Exception) {
    println("[BadgeUtils]: Error setting badge: ${e.message}")
  }
}

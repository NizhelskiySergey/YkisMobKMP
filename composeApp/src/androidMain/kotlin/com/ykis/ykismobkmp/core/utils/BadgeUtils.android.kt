package com.ykis.ykismobkmp.core.utils


import android.util.Log

actual fun applyAppBadgeCount(count: Int) {
  // На Android установка баджа часто требует сторонних библиотек (типа ShortcutBadger),
  // так как в чистом Android SDK до API 26 (Oreo) этого нет.
  // Пока оставим лог или реализацию через уведомления.
  Log.d("YkisLog", "[BadgeUtils]: Setting Android badge to $count")
}

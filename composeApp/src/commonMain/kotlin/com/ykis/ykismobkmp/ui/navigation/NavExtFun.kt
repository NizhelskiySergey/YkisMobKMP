package com.ykis.ykismobkmp.ui.navigation


import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator

private const val className = "NavigationExtensions"

/**
 * [cleanNavigateTo] — Полная очистка стека навигации ЮКИС и переход на новый корневой экран.
 * ИСПРАВЛЕНО: Заменен NavHostController. Нативно очищает стек Voyager (аналог popUpTo(0) { inclusive = true }).
 */
fun Navigator.cleanNavigateTo(screen: Screen) {
  println("[$className.cleanNavigateTo]: Очищення стеку та перехід на новий корінь: ${screen::class.simpleName}")

  // Метод deleteAllUpdates() или replaceAll() полностью сбрасывает историю переходов,
  // предотвращая утечки памяти при Logout или смене лицевого счета БТИ.
  this.replaceAll(screen)
}

/**
 * [navigateWithPopUp] — Переход на экран с возможностью частичного удаления стека до определенной точки.
 * ИСПРАВЛЕНО: Реализовано через безопасные КМР-манипуляции со стеком Voyager Screen.
 */
fun Navigator.navigateWithPopUp(targetScreen: Screen, popUpToScreen: Screen? = null) {
  println("[$className.navigateWithPopUp]: Перехід на ${targetScreen::class.simpleName}")

  if (popUpToScreen != null) {
    // Извлекаем текущий стек экранов, фильтруем его до нужной точки и добавляем новый экран
    val currentItems = this.items
    val index = currentItems.indexOf(popUpToScreen)
    if (index != -1) {
      val truncatedList = currentItems.subList(0, index + 1)
      this.replaceAll(truncatedList + targetScreen)
      return
    }
  }

  // Если точка сброса стека не найдена или не передана — выполняем стандартный накат окна (push)
  this.push(targetScreen)
}

/**
 * [navigateToInfoApartment] — Быстрый переход на главный информационный экран характеристик БТИ квартиры.
 * ИСПРАВЛЕНО: Стерты строковые роуты, вызов переведен на нативный KMP-объект экрана.
 */
fun Navigator.navigateToInfoApartment(infoScreen: Screen) {
  println("[$className.navigateToInfoApartment]: Каскадне повернення на головне вікно БТІ")

  // В Voyager для синглтонов навигации используется замена текущего окна (replace)
  // либо push с ручным контролем дубликатов, что исключает Race Condition
  this.replaceAll(infoScreen)
}

package com.ykis.ykismobkmp.ui.navigation

import kotlinx.serialization.Serializable

/**
 * [AppStartState] — Нативные КМР-фазы холодного старта приложения ЮКИС.
 * ИСПРАВЛЕНО: Упорядочены фазы навигационного автомата для сквозного разделения ролей жильцов и админов.
 * Зафиксирован для полной замены.
 */
@Serializable
sealed class AppStartState {

  @Serializable
  data object Loading : AppStartState()

  @Serializable
  data object TermsAndConditions : AppStartState()

  @Serializable
  data object SignIn : AppStartState()

  @Serializable
  data object AddApartment : AppStartState()

  @Serializable
  data object InfoApartment : AppStartState()

  @Serializable
  data object UserList : AppStartState()
}

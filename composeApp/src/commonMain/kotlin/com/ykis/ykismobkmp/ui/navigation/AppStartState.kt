package com.ykis.ykismobkmp.ui.navigation

import kotlinx.serialization.Serializable

/**
 * [AppStartState] — Нативные КМР-фазы холодного старта приложения ЮКИС.
 */
@Serializable
sealed class AppStartState {
  data object Loading : AppStartState()
  data object TermsAndConditions : AppStartState()
  data object SignIn : AppStartState()
  data object AddApartment : AppStartState()
  data object InfoApartment : AppStartState()
  data object UserList : AppStartState()
}

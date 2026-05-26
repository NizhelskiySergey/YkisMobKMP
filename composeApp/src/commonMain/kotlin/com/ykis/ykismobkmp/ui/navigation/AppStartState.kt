package com.ykis.ykismobkmp.ui.navigation

import kotlinx.serialization.Serializable
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

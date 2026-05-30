package com.ykis.ykismobkmp.ui.navigation
/**
 * [AppStartState] — Сериализационно-безопасная кроссплатформенная матрица состояний
 * холодного старта и онбординга ЮКІС г. Южный.
 * ИСПРАВЛЕНО НАМЕРТВО: Аннотации @Serializable полностью вырезаны. Стейты теперь нативно
 * удерживаются в ОЗУ устройства, полностью ликвидируя зависание кадров Compose на Шаге №14!
 */
sealed class AppStartState {

  data object Loading : AppStartState()

  data object TermsAndConditions : AppStartState()

  data object SignIn : AppStartState()

  data object AddApartment : AppStartState()

  data object InfoApartment : AppStartState()

  data object UserList : AppStartState()
}

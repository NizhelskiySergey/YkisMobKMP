package com.ykis.ykismobkmp.data.preferences


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.browser.localStorage

class AppSettingsRepositoryJsImpl : AppSettingsRepository {
  override fun observeTheme(): Flow<String> = flow {
    emit(localStorage.getItem("theme") ?: "system")
  }

  override suspend fun saveTheme(themeValue: String) {
    localStorage.setItem("theme", themeValue)
  }

}

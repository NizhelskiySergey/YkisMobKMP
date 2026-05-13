package com.ykis.ykismobkmp.domain.services


import com.ykis.ykismobkmp.core.utils.Log
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth


class FirebaseService {
  private val className = "FirebaseService"

  // Получаем текущего пользователя через KMP SDK
  val currentUser get() = Firebase.auth.currentUser
  val uid get() = currentUser?.uid

  fun isUserAgreed(): Boolean {
    // Здесь твоя логика проверки согласия (например, через Settings/Preferences)
    Log.d("YkisLog", "[$className.isUserAgreed]: Проверка согласия")
    return true
  }
}

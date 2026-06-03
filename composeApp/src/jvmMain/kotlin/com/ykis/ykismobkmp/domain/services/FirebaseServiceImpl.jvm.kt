package com.ykis.ykismobkmp.domain.services

import com.ykis.ykismobkmp.core.utils.Resource

// ИСПРАВЛЕНО: Сигнатура синхронизирована по параметрам
actual suspend fun performPlatformSendSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  phoneNumber: String,
  platformActivity: Any?
): Resource<String> {
  println("[YkisLogKMP.FirebaseServiceImpl]: [JVM_DESKTOP] Сценарий SMS недоступен на Mac/PC")
  return Resource.Error("Вхід за номером телефону підтримується тільки на мобільних пристроях Android та iOS.")
}
actual suspend fun performPlatformSignInWithSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  verificationId: String,
  smsCode: String
): Resource<Boolean> {
  println("[YkisLogKMP.FirebaseServiceImpl]: [JVM_DESKTOP] Сценарій входу за SMS недоступний на Mac/PC")
  return Resource.Error("Функція авторизації за номером телефону недоступна на десктопних платформах.")
}

actual suspend fun getPlatformFcmToken(): String? = null

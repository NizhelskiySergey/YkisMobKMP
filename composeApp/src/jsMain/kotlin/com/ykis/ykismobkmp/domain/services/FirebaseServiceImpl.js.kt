package com.ykis.ykismobkmp.domain.services

import com.ykis.ykismobkmp.core.utils.Resource

// ИСПРАВЛЕНО: Сигнатура синхронизирована по параметрам
actual suspend fun performPlatformSendSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  phoneNumber: String,
  platformActivity: Any?
): Resource<String> {
  println("[YkisLogKMP.FirebaseServiceImpl]: [JS_WEB] Сценарий SMS изолирован для Web-браузера")
  return Resource.Error("Вхід за номером телефону тимчасово обмежений у Веб-версії.")
}
actual suspend fun performPlatformSignInWithSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  verificationId: String,
  smsCode: String
): Resource<Boolean> {
  println("[YkisLogKMP.FirebaseServiceImpl]: [JS_WEB] Сценарій входу за SMS ізольований для браузера")
  return Resource.Error("Функція авторизації за номером телефону недоступна у Веб-версії.")
}

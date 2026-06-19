package com.ykis.ykismobkmp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.ykis.ykismobkmp.di.initAndroidKoin

/**
 * [YkisApp] — Главный входной узел приложения на платформе Android.
 */
class YkisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Инициализируем Koin ПЕРВЫМ (чтобы все зависимости были готовы до старта сервисов)
        initAndroidKoin(this)

        // 2. Инициализируем Firebase
        try {
            FirebaseApp.initializeApp(this)
            
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

            if (isDebug) {
                println("[YkisLogKMP.YkisApp]: Режим ОТЛАДКИ. Использование DebugAppCheckProvider.")
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                // Виводимо токен в лог для зручності реєстрації в консолі
                firebaseAppCheck.getAppCheckToken(false).addOnSuccessListener { token ->
                    println("[YkisLogKMP.AppCheck]: Ваш Debug Token для Firebase Console: ${token.token}")
                }
            } else {
                println("[YkisLogKMP.YkisApp]: РЕЛИЗНЫЙ режим. Использование PlayIntegrity.")
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
            
            // "Прогрев" токена в фоновом режиме
            firebaseAppCheck.getAppCheckToken(false)
            
        } catch (e: Exception) {
            println("[YkisLogKMP.YkisApp_ERROR]: Ошибка при инициализации Firebase: ${e.message}")
        }
        
        println("[YkisLogKMP.YkisApp]: Инициализация завершена успешно.")
    }
}

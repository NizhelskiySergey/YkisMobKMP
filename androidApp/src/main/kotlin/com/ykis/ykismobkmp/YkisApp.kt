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
        
        // 1. Инициализируем Koin
        initAndroidKoin(this)

        // 2. Инициализируем Firebase
        try {
            FirebaseApp.initializeApp(this)
            
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

            if (isDebug) {
                println("[YkisLogKMP.AppCheck]: Режим ОТЛАДКИ. Установка DebugProvider...")
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                
                // ГАРАНТОВАНИЙ ВИВІД ТОКЕНА ДЛЯ КОНСОЛІ
                firebaseAppCheck.getAppCheckToken(false).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        println("==========================================================================")
                        println("[YkisLogKMP.AppCheck]: ВАШ DEBUG TOKEN:")
                        println("${task.result.token}")
                        println("==========================================================================")
                    } else {
                        println("[YkisLogKMP.AppCheck_ERROR]: Не вдалося отримати токен: ${task.exception?.message}")
                    }
                }
            } else {
                println("[YkisLogKMP.YkisApp]: РЕЛИЗНЫЙ режим. Использование PlayIntegrity.")
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
            
        } catch (e: Exception) {
            println("[YkisLogKMP.YkisApp_ERROR]: Ошибка при инициализации Firebase: ${e.message}")
        }
    }
}

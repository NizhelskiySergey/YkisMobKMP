package com.ykis.ykismobkmp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.ykis.ykismobkmp.di.initAndroidKoin

/**
 * [YkisApp] — Главный входной узел приложения на платформе Android.
 */
class YkisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Базовая инициализация Firebase
        FirebaseApp.initializeApp(this)

        // 2. Установка App Check (Debug) для прохождения фильтров спама Google
        val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // 3. Запуск DI ядра
        initAndroidKoin(this)
        
        println("[YkisLogKMP.YkisApp]: Firebase App Check активен. Ядро KMP запущено.")
    }
}

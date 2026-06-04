package com.ykis.ykismobkmp

import android.app.Application
import com.ykis.ykismobkmp.di.initAndroidKoin

/**
 * [YkisApp] — Главный входной узел приложения на платформе Android.
 * ИСПРАВЛЕНО: Инициализация DI-графа перенесена сюда. Это гарантирует, что Koin будет готов
 * ДО старта любых Activity или Сервисов (включая MyFirebaseMessagingService).
 * Это устраняет риск падения при получении пуша во время холодного старта.
 */
class YkisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем Koin один раз при запуске процесса
        initAndroidKoin(this)
        
        println("[YkisLogKMP.YkisApp]: Ядро KMP успешно инициализировано на уровне Application")
    }
}

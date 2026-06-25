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
        initAndroidKoin(this)

        try {
            FirebaseApp.initializeApp(this)
            
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

            if (isDebug) {
                println("[YkisLogKMP.AppCheck]: РЕЖИМ ОТЛАДКИ")
                firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                
                firebaseAppCheck.getAppCheckToken(false).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        println("==========================================================================")
                        println("[YkisLogKMP.AppCheck]: ВАШ DEBUG TOKEN ДЛЯ FIREBASE CONSOLE:")
                        println(task.result.token)
                        println("==========================================================================")
                    }
                }
            } else {
                firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
            
        } catch (e: Exception) {
            println("[YkisLogKMP.YkisApp_ERROR]: ${e.message}")
        }
    }
}

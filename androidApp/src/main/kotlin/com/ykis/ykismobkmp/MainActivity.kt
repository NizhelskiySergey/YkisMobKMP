package com.ykis.ykismobkmp

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.russhwolf.settings.SharedPreferencesSettings
import java.util.Locale

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Установка локали перед setContent
        updateLocale(this)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            YkisPamAppRoot(
                windowSize = windowSizeClass,
                displayFeatures = emptyList()
            ) 
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateLocale(newBase))
    }

    private fun updateLocale(context: Context): Context {
        // Читаем язык из настроек. Если пусто - ставим "uk" (Украинский) по умолчанию.
        val prefs = context.getSharedPreferences("com.ykis.ykismobkmp_preferences", Context.MODE_PRIVATE)
        val settings = SharedPreferencesSettings(prefs)
        val lang = settings.getString("app_language", "uk")
        
        val locale = Locale(lang)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        return context.createConfigurationContext(config)
    }
}

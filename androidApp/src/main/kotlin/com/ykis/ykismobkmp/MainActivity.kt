package com.ykis.ykismobkmp

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.content.ContextCompat
import com.russhwolf.settings.SharedPreferencesSettings
import java.util.Locale

@SuppressLint("AppBundleLocaleChanges")
class MainActivity : ComponentActivity() {
    
    // ДОДАНО: Системний лаунчер для запиту дозволу на сповіщення (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            println("[YkisLogKMP.MainActivity]: Дозвіл на сповіщення отримано.")
        } else {
            println("[YkisLogKMP.MainActivity]: Користувач відмовив у сповіщеннях.")
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Установка локали перед setContent
        updateLocale(this)

        // ДОДАНО: Запит дозволу на сповіщення при старті (тільки для Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }

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
        val prefs = context.getSharedPreferences("com.ykis.ykismobkmp_preferences", MODE_PRIVATE)
        val settings = SharedPreferencesSettings(prefs)
        val lang = settings.getString("app_language", "uk")
        
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        return context.createConfigurationContext(config)
    }
}

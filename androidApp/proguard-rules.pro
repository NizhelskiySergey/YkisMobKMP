# Базовые правила для Android
-keepattributes *Annotation*, EnclosingMethod, Signature, InnerClasses

# Compose Multiplatform
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }

# Kotlin Serialization (сохраняем модели данных)
-keepnames class kotlinx.serialization.json.internal.** { *; }
-keepclassmembers class com.ykis.ykismobkmp.** {
    *** get*();
    *** set*(***);
}
-keep class com.ykis.ykismobkmp.data.models.** { *; }
-keep class com.ykis.ykismobkmp.domain.entity.** { *; }
-keep class com.ykis.ykismobkmp.data.responses.** { *; }

# Koin
-keep class io.insertkoin.** { *; }

# Voyager (Навигация)
-keep class cafe.adriel.voyager.** { *; }

# SQLDelight
-keep class com.ykis.ykismobkmp.db.** { *; }
-keep class app.cash.sqldelight.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class dev.gitlive.firebase.** { *; }

# Google Sign-In & Credential Manager
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# Сохраняем инициализаторы Startup
-keep class * extends androidx.startup.Initializer { *; }

# Compose Resources
-keep class com.ykis.ykismobkmp.Res { *; }
-keep class com.ykis.ykismobkmp.Res$* { *; }

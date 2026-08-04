# Базовые правила для Compose
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView { *; }

# Kotlin Serialization (сохраняем модели данных)
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepnames class kotlinx.serialization.json.internal.** { *; }
-keep class com.ykis.ykismobkmp.data.models.** { *; }
-keep class com.ykis.ykismobkmp.domain.entity.** { *; }
-keep class com.ykis.ykismobkmp.data.responses.** { *; }

# Koin
-keep class io.insertkoin.** { *; }

# SQLDelight
-keep class com.ykis.ykismobkmp.db.** { *; }
-keep class app.cash.sqldelight.** { *; }

# Firebase (обычно правила подтягиваются сами, но для надежности)
-keep class com.google.firebase.** { *; }
-keep class dev.gitlive.firebase.** { *; }

# Предотвращаем удаление ресурсов Compose
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.resources.** { *; }

# Сохраняем инициализаторы для androidx.startup
-keep class * extends androidx.startup.Initializer { *; }

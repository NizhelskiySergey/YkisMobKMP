# 1. Глобальная оптимизация
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-mergeinterfacesaggressively

# 2. Атрибуты
-keepattributes *Annotation*, EnclosingMethod, Signature, InnerClasses

# 3. Compose Multiplatform (Более точные правила)
-keep class androidx.compose.runtime.ParcelableSnapshotMutationPolicy { *; }
-keep class androidx.compose.ui.platform.AndroidComposeView { *; }
-keep class org.jetbrains.compose.resources.** { *; }

# 4. Kotlin Serialization (Критично для работы API)
-keepattributes *Annotation*, Signature
-keepclassmembers class com.ykis.ykismobkmp.data.models.** {
    *** get*();
    *** set*(***);
}
-keep class com.ykis.ykismobkmp.data.models.** { *; }
-keep class com.ykis.ykismobkmp.domain.entity.** { *; }
-keep class com.ykis.ykismobkmp.data.responses.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# 5. Koin (Разрешаем обфускацию, сохраняем только жизненно важное)
-keepclassmembers class * {
    @org.koin.core.annotation.KoinInternalApi *;
}
-keepnames class io.insertkoin.** { *; }

# 6. SQLDelight (Сохраняем только сгенерированные драйверы и адаптеры)
-keep class com.ykis.ykismobkmp.db.** { *; }
-keepnames class app.cash.sqldelight.adapter.** { *; }
-keepnames class app.cash.sqldelight.driver.android.** { *; }

# 7. Firebase & Google Auth
-keep class dev.gitlive.firebase.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# 8. Compose Resources
-keep class com.ykis.ykismobkmp.Res { *; }
-keep class com.ykis.ykismobkmp.Res$* { *; }

# 9. Сохраняем Startup Initializers
-keep class * extends androidx.startup.Initializer { *; }

# 10. Coil (Убираем предупреждения)
-dontwarn coil3.**
-dontwarn coil.**

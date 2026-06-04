rootProject.name = "YkisMobKMP"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()

  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    // Дополнительный репозиторий JetBrains для KMP-ресурсов
    maven("https://maven.pkg.jetbrains.space/public/p/compose/patch")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")

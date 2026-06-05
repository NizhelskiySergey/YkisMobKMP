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
    // Временно отключаем нестабильный репозиторий JetBrains, 
    // так как он вызывает сбои при поиске Firebase библиотек.
    // maven("https://maven.pkg.jetbrains.space/public/p/compose/patch")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")
include(":androidApp")

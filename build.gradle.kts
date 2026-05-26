plugins {
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.composeHotReload) apply false
  alias(libs.plugins.composeMultiplatform) apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.sqldelight) apply false
  alias(libs.plugins.googleServices) apply false
  alias(libs.plugins.crashlytics) apply false

  // ИСПРАВЛЕНО: Каноническое подключение плагина через встроенный маркер Gradle
//  id("com.github.gmazzo.buildkonfig") version "0.15.2" apply false
}

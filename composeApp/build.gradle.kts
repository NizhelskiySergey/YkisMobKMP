import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary) 
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.composeHotReload)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.sqldelight)
}

// РУЧНА ГЕНЕРАЦІЯ КОНФІГУРАЦІЇ ВЕРСІЇ
val generateAppConfig by tasks.registering {
    val version = libs.versions.app.version.get()
    val versionCode = libs.versions.app.versionCode.get()
    val outputDir = layout.buildDirectory.dir("generated/ykis/commonMain/kotlin")
    inputs.property("version", version)
    inputs.property("versionCode", versionCode)
    outputs.dir(outputDir)

    doLast {
        val configFile = outputDir.get().file("com/ykis/ykismobkmp/AppConfig.kt").asFile
        configFile.parentFile.mkdirs()
        configFile.writeText("""
            package com.ykis.ykismobkmp

            object AppConfig {
                const val APP_VERSION = "$version"
                const val APP_VERSION_CODE = $versionCode
            }
        """.trimIndent())
    }
}

kotlin {
  androidTarget {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_21)
      freeCompilerArgs.add("-Xexpect-actual-classes")
    }
  }

  iosArm64()
  iosSimulatorArm64()

  jvm {
    compilerOptions {
      freeCompilerArgs.add("-Xexpect-actual-classes")
    }
  }

  js {
    compilerOptions {
      freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    outputModuleName.set("composeApp")
    useEsModules()

    browser {
      commonWebpackConfig {
        outputFileName = "composeApp.js"

        devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).copy(
          open = true,
          port = 8081
        )
      }
    }
    binaries.executable()
  }

  sourceSets {
    commonMain {
      kotlin.srcDir(generateAppConfig.map { it.outputs.files.asPath })
      dependencies {
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.material3.windowSizeClass)
        implementation(libs.material.icons.extended)
        implementation(libs.compose.ui)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.compose.components.resources)

        implementation(libs.androidx.lifecycle.viewmodelCompose)
        implementation(libs.androidx.lifecycle.runtimeCompose)
        implementation(libs.koin.core)
        implementation(libs.koin.compose)
        implementation(libs.koin.compose.viewmodel)

        implementation(libs.voyager.navigator)
        implementation(libs.voyager.tab.navigator)
        implementation(libs.voyager.screenmodel)
        implementation(libs.voyager.koin)
        implementation(libs.voyager.transitions)

        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.json)
        implementation(libs.ktor.client.logging)
        implementation(libs.kotlinx.serialization.json)

        implementation(libs.sqldelight.runtime)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.sqldelight.primitive.adapters)

        implementation(libs.firebase.common)
        implementation(libs.firebase.auth)
        implementation(libs.firebase.functions)
        implementation(libs.firebase.config)
        implementation(libs.firebase.messaging)
        implementation(libs.firebase.firestore)
        implementation(libs.firebase.database)
        implementation(libs.firebase.storage)
        implementation(libs.multiplatform.settings)
        implementation(libs.multiplatform.settings.no.arg)

        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
        implementation(libs.napier)
      }
    }

    androidMain.dependencies {
      implementation(project.dependencies.platform(libs.firebase.bom))
      implementation(libs.firebase.ai)
      implementation(libs.androidx.activity.compose)
      implementation(libs.androidx.splashscreen)
      implementation(libs.ktor.client.okhttp)
      implementation(libs.koin.android)
      implementation(libs.sqldelight.android)
      implementation(libs.compose.uiTooling)
      implementation(libs.firebase.crashlytics)
      implementation(libs.firebase.analytics)
      implementation(libs.firebase.common.ktx)
      implementation(libs.androidx.preference.ktx)
      implementation(libs.androidx.ui.viewbinding)
      implementation(libs.androidx.camera.core)
      implementation(libs.androidx.camera.camera2)
      implementation(libs.androidx.camera.lifecycle)
      implementation(libs.androidx.camera.view)
      implementation(libs.androidx.window)
      implementation(libs.firebase.appcheck.debug)
      implementation(libs.googleid)
      implementation(libs.androidx.credentials)
      implementation(libs.androidx.credentials.play.services.auth)
      implementation(libs.androidx.datastore.preferences.core)
      implementation(libs.androidx.datastore.core)
      implementation(libs.play.services.auth.api.phone)
    }

    val iosMain by creating {
      dependsOn(commonMain.get())
      dependencies {
        implementation(libs.ktor.client.darwin)
        implementation(libs.firebase.crashlytics)
        implementation(libs.native.driver)
        implementation(libs.androidx.datastore.preferences.core)
        implementation(libs.androidx.datastore.core)
      }
    }
    
    val iosArm64Main by getting { dependsOn(iosMain) }
    val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutinesSwing)
      implementation(libs.sqldelight.jvm)
      implementation(libs.ktor.client.java)
      implementation(libs.ktor.client.okhttp)
      implementation(libs.ktor.client.cio)
      implementation(libs.webcam.capture)
      implementation(libs.androidx.datastore.preferences.core)
      implementation(libs.androidx.datastore.core)
    }

    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(libs.ktor.client.js)
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui)
        implementation(libs.sqldelight.web)
        implementation(npm("sql.js", "1.10.3"))
      }
    }
  }
}

android {
  namespace = "com.ykis.ykismobkmp.compose"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      isShrinkResources = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

compose.desktop {
  application {
    mainClass = "com.ykis.ykismobkmp.MainKt"
    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "com.ykis.ykismobkmp"
      packageVersion = libs.versions.app.version.get()
    }
  }
}

sqldelight {
  databases {
    create("YkisDatabases") {
      packageName.set("com.ykis.ykismobkmp.db")
      generateAsync.set(true)
    }
  }
}

compose {
  resources {
    packageOfResClass = "ykismobkmp.composeapp.generated.resources"
  }
}

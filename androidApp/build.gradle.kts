import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    id("org.jetbrains.kotlin.android") // Возвращаем плагин для поддержки src/main/kotlin
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.ykis.ykismobkmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ykis.ykismobkmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.app.versionCode.get().toInt()
        versionName = libs.versions.app.version.get()
    }

    // Явно указываем, где лежит ваш код
    sourceSets {
        getByName("main") {
            java.setSrcDirs(listOf("src/main/kotlin", "src/main/java"))
        }
    }

    val props = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { props.load(it) }
    }

    signingConfigs {
        create("release") {
            val path = props.getProperty("signing.keyStorePath")
            if (path != null) {
                val storeFileObj = file(path)
                if (storeFileObj.exists()) {
                    storeFile = storeFileObj
                    storePassword = props.getProperty("signing.keyStorePassword")
                    keyAlias = props.getProperty("signing.keyAlias")
                    keyPassword = props.getProperty("signing.keyPassword")
                    println("[YkisLogKMP]: Signing config 'release' successfully initialized.")
                }
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.startup)
    implementation(libs.compose.material3.windowSizeClass)
    implementation(libs.koin.android)
    implementation(libs.voyager.screenmodel)
    implementation(libs.multiplatform.settings)
    implementation(libs.compose.components.resources)
    
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging.native)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.ai)
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
}

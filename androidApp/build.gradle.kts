import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    id("org.jetbrains.kotlin.android")
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

    bundle {
        language {
            enableSplit = false
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
                } else {
                    println("[YkisLogKMP_ERROR]: JKS file not found at path: $path")
                }
            } else {
                println("[YkisLogKMP_ERROR]: 'signing.keyStorePath' is missing in local.properties")
            }
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin", "src/main/java")
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
            
            // Включаем генерацию и упаковку нативных отладочных символов для Google Play
            // Это упакует символы прямо внутрь .aab файла
            ndk {
                debugSymbolLevel = "FULL"
            }

            // Настройка Firebase Crashlytics
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
                unstrippedNativeLibsDir = file("build/intermediates/merged_native_libs/release/out/lib")
            }
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
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
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging.native)
    implementation(libs.firebase.appcheck.debug)
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
}

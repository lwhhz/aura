import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 起内建 Kotlin 支持，不再应用 org.jetbrains.kotlin.android
    alias(libs.plugins.kotlin.compose)
}

android {
    // Compose 1.13（compose-bom-alpha 2026.11.00）要求 compileSdk >= 37.1
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        namespace = "com.aura.chat"
        applicationId = "com.aura.chat"
        minSdk = 26
        targetSdk = 36
        versionCode = 45
        versionName = "1.1.3"
    }

    signingConfigs {
        create("release") {
            // 口令不进版本库：从 keystore.properties 读取（本机文件，已从交付文档中隐去）
            val propsFile = rootProject.file("keystore.properties")
            val props = Properties().apply {
                if (propsFile.exists()) propsFile.inputStream().use { load(it) }
            }
            storeFile = rootProject.file(props.getProperty("storeFile", "release.keystore"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias", "aura")
            keyPassword = props.getProperty("keyPassword")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)


    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
}
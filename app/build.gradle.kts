plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.deylin.ganfan"
    compileSdk = 34

    // 固定签名：与 AutoLedger 共用同一把 key，覆盖安装不丢数据
    signingConfigs {
        create("fixed") {
            storeFile = file("../keystore.p12")
            storePassword = "autoledger2026"
            keyAlias = "autoledger"
            keyPassword = "autoledger2026"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fixed")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("fixed")
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    defaultConfig {
        applicationId = "com.deylin.ganfan"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

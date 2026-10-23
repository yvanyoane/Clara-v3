plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.yvan.clara"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yvan.clara"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "2.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

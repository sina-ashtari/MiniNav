plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "app.mininav"
    compileSdk = 36
    defaultConfig {
        applicationId = providers.gradleProperty("mininav.applicationId").getOrElse("app.mininav")
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    val store = System.getenv("MININAV_KEYSTORE")
    if (store != null) signingConfigs.create("publisher") {
        storeFile = file(store)
        storePassword = System.getenv("MININAV_STORE_PASSWORD")
        keyAlias = System.getenv("MININAV_KEY_ALIAS")
        keyPassword = System.getenv("MININAV_KEY_PASSWORD")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (store != null) signingConfig = signingConfigs.getByName("publisher")
        }
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.09.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}

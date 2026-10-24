import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Google Maps key lives in local.properties (MAPS_API_KEY=AIza...), never in Git.
// Without a key the app falls back to the free OpenStreetMap map.
val mapsApiKey: String = Properties().run {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
    getProperty("MAPS_API_KEY", "")
}

android {
    namespace = "com.laundryhub.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.laundryhub.mobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // lets us use java.time (LocalDateTime) on Android 7
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Jetpack Compose (UI)
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Talking to the Spring Boot API
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Free OpenStreetMap map (FR-08 live driver location)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Smooth vector map for the location picker (free OpenFreeMap tiles, no key, no card)
    implementation("org.maplibre.gl:android-sdk:11.13.5")

    // Google Maps (location picker) - used instead when MAPS_API_KEY is set
    implementation("com.google.maps.android:maps-compose:6.2.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
}

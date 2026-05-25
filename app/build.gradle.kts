import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

// Read the Ollama API key from local.properties and inject as a BuildConfig field
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val ollamaApiKey: String = localProperties.getProperty("OLLAMA_API_KEY") ?: ""
val plantIdApiKey: String = localProperties.getProperty("PLANT_ID_API_KEY") ?: ""

android {
    namespace = "com.growguide.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.growguide.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Expose the API key as a BuildConfig constant
        buildConfigField("String", "OLLAMA_API_KEY", "\"$ollamaApiKey\"")
        buildConfigField("String", "PLANT_ID_API_KEY", "\"$plantIdApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX core libraries
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Material Design 3
    implementation("com.google.android.material:material:1.12.0")

    // Firebase BOM - imports all Firebase version aligned together
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // OkHttp for API calls to Ollama
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.11.0")

    // Markwon for rendering markdown in chat bubbles
    implementation("io.noties.markwon:core:4.6.2")

    // MPAndroidChart for growth timeline visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}

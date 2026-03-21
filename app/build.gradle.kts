plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.aitaskgenius"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aitaskgenius"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // LIBRERÍAS BASE ANDROID
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // LIBRERÍA GSON
    implementation("com.google.code.gson:gson:2.11.0")
    // implementation(libs.generativeai) // Usaremos la versión directa abajo para evitar conflictos

    // LIBRERÍAS DE ROOM
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // --- CONFIGURACIÓN DE FIREBASE (Usando BoM para evitar errores de versión) ---
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore")


    // LIBRERÍAS DE CREDENTIALS (Google Sign In Moderno)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // TEST
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.firebase.crashlytics.buildtools)

    // LIBRERÍAS PARA COMUNICACIÓN HTTP
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // SDK de Google AI para usar Gemini (IMPORTANTE: Usar este y no google-genai)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}
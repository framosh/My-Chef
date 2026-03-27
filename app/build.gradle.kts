import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
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
        
        // CARGA ROBUSTA DE PROPIEDADES
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        
        // Solo mantenemos UnsplashKey si decides dejarla local, 
        // aunque lo ideal es que también pase al servidor más adelante.
        val unsplashKey = localProperties.getProperty("UNSPLASH_ACCESS_KEY") ?: ""
        
        // Inyectamos solo las claves necesarias en BuildConfig
        buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"$unsplashKey\"")
        
        if (unsplashKey.isNotEmpty()) {
            println("CONFIRMACIÓN: Unsplash Key cargada correctamente")
        }
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.google.code.gson:gson:2.11.0")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.firebase.crashlytics.buildtools)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // Nota: Podrías eliminar 'generativeai' si ya no usas el SDK directo, 
    // pero lo dejamos por si necesitas otras funciones de IA en el futuro.
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}
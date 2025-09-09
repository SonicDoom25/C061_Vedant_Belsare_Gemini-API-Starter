plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.c061_vedant_belsare.geminiapistarter"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.c061_vedant_belsare.geminiapistarter"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Correct way to reference API key from gradle.properties
        buildConfigField(
            "String",
            "API_KEY",
            "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.common)
    implementation(libs.generativeai)

    // ✅ Room dependencies (for Java)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Optional - only if you ever add Kotlin coroutines/Flow support
    // implementation("androidx.room:room-ktx:2.6.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

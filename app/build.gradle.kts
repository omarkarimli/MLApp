plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.omarkarimli.mlapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omarkarimli.mlapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Material Icons
    implementation(libs.androidx.material.icons.extended)

    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // ML Kit Barcode Scanning
    implementation(libs.barcode.scanning)

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Image Labeling
    implementation("com.google.mlkit:image-labeling:17.0.9")

    // Object Detection
    implementation("com.google.mlkit:object-detection:17.0.2")

    // Face Mesh Detection
    implementation("com.google.mlkit:face-mesh-detection:16.0.0-beta3")

    // Face Detection
    implementation("com.google.mlkit:face-detection:16.1.7")

    // Text Recognition
    // Latin
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // Chinese
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    // Devanagari
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
    // Japanese
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
    // Korean
    implementation("com.google.mlkit:text-recognition-korean:16.0.1")
}
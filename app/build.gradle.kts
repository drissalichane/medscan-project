plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.ocr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ocr"
        minSdk = 24
        targetSdk = 35
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Retrofit
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Gson Converter
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0") // Logging Interceptor (optional)

    implementation("androidx.core:core-ktx:1.12.0") // Ensure AndroidX compatibility
    implementation("com.google.android.material:material:1.10.0") // Material UI (optional)
    implementation("androidx.activity:activity-ktx:1.7.2") // Activity KTX for ActivityResultLauncher
    implementation("androidx.exifinterface:exifinterface:1.3.6") // ExifInterface for image orientationdv
    implementation("com.google.mlkit:text-recognition:16.0.0") // ML Kit OCR

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

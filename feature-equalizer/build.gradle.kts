plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Required for Kotlin 2.0+ Compose plugin
    alias(libs.plugins.ksp) // For Hilt annotation processing if used in core
    alias(libs.plugins.hilt.android) // Hilt plugin for the core module
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.engfred.musicplayer.feature_equalizer"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true // Enable Compose
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeBom.get()
    }
}

dependencies {

    // Android Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose) // For ViewModels in Compose

    implementation(project(":core"))

    // Jetpack Compose UI (for shared UI components like themes, common Composables)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Hilt (if core module provides app-wide singletons or common dependencies)
    // Hilt (Dependency Injection for this feature)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui) // For player UI components if needed
    implementation(libs.media3.session) // For background playback and notification integration

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    //
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
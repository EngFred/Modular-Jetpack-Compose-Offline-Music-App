plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.engfred.musicplayer.feature_player"
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

    // Dependency on the core module for shared utilities, themes, etc.
    implementation(project(":core"))

    // Android Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose) // For ViewModels in Compose

    // Jetpack Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose) // If this feature has internal navigation
    implementation(libs.androidx.material.icons.extended) // For Material Icons

    // Hilt (Dependency Injection for this feature)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Media3 (Crucial for music playback) - Updated to latest
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // For MediaSessionCompat (compatibility for MediaStyle)
    implementation(libs.androidx.media)

    // Guava for ImmutableList
    implementation(libs.guava)

    // DataStore (if player settings need to be persisted)
    implementation(libs.androidx.datastore.preferences)

    // Coil for album artwork
    implementation(libs.coil.compose)
    // Landscapist with Coil engine
    implementation(libs.landscapist.coil)

    //
    implementation(libs.androidx.palette.ktx)

    //
    implementation(libs.accompanist.systemuicontroller)

    //
//    implementation(libs.androidx.adaptive)
//    implementation(libs.androidx.material3.window.size.class1.android)

    implementation(libs.kotlinx.coroutines.guava)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
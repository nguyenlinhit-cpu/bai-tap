plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.baiviet.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.baiviet.game"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
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
        compose = true
    }
}

dependencies {
    // ── Project modules ──
    implementation(project(":core:cards"))
    implementation(project(":core:engine"))
    implementation(project(":core:ai"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":game:tienlen"))
    implementation(project(":game:samloc"))
    implementation(project(":game:phom"))
    implementation(project(":game:maubinh"))
    implementation(project(":game:xidach"))
    implementation(project(":game:poker"))
    implementation(project(":game:lieng"))
    implementation(project(":game:bacay"))

    // ── AndroidX ──
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.compose)

    // ── Compose ──
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ── DI ──
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── Serialization ──
    implementation(libs.kotlinx.serialization)

    // ── Media ──
    implementation(libs.lottie.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    // ── Testing ──
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

tasks.register<Copy>("exportApks") {
    dependsOn("assembleRelease", "assembleDebug")
    into(rootProject.file("output"))
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("app-release.apk")
        rename("app-release.apk", "BaiViet-1.0.0-release.apk")
    }
    from(layout.buildDirectory.dir("outputs/apk/debug")) {
        include("app-debug.apk")
        rename("app-debug.apk", "BaiViet-1.0.0-debug.apk")
    }
}

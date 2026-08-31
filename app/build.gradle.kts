plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.voltcare.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.voltcare.app"
        minSdk = 29
        targetSdk = 34
        // Batch 65 (revisi RULE Batch 37): versionCode kini AUTO dari GITHUB_RUN_NUMBER -
        // dijamin selalu naik tiap build CI, tidak perlu bump manual lagi per batch. Fallback
        // "1" hanya kepakai kalau build lokal non-CI (jarang - build resmi selalu lewat Actions).
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.0.54"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Batch 36: dibaca UpdateManager.kt buat bedain build CI mana yang lebih baru walau
        // versionName BELUM di-bump (kasus umum: banyak batch fix/patch numpuk antar bump
        // versi manual, lihat laporan bug "Sudah Versi Terbaru" padahal ada build hijau baru).
        // GITHUB_RUN_NUMBER otomatis tersedia di semua step Actions & PERSIS sama dgn angka
        // yang dipakai release.yml buat tag_name (v{version}-{run_number}), jadi selalu
        // konsisten tanpa perlu ubah release.yml. Default "0" utk build lokal/non-CI.
        buildConfigField("String", "CI_RUN_NUMBER", "\"${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("ANDROID_KEYSTORE_PATH") ?: "keystore/release.keystore"
            // rootProject.file() dipakai (bukan file()) karena file() di module build.gradle.kts
            // resolve relatif ke app/, bukan root repo -> keystore/release.keystore tidak pernah
            // ketemu -> signingConfig silent-skip -> AGP keluarkan app-release-unsigned.apk.
            val f = rootProject.file(storeFilePath)
            if (f.exists()) {
                storeFile = f
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val storeFilePath = System.getenv("ANDROID_KEYSTORE_PATH") ?: "keystore/release.keystore"
            if (rootProject.file(storeFilePath).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}

// room.schemaLocation (Pending Queue #7): AppDatabase punya exportSchema=true sejak Batch 1
// tapi tanpa arg ini KSP hanya warning tanpa pernah menulis file JSON skema. Diset ke
// app/schemas/ (dalam module app, bukan root) - dicommit ke Git sbg riwayat migrasi formal,
// BUKAN build output sehingga aman, tidak masuk cakupan .gitignore (/build) yang sudah ada.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    // WorkManager (scheduled log retention / periodic checks - used by service layer)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // In-app updater (UpdateManager): streaming download chunk-by-chunk via Okio sink,
    // timeout eksplisit, followRedirects — sesuai Release Downloader Spec (PROJECT_STATE).
    // Okio dibawa transitif oleh OkHttp, tidak perlu dideklarasikan terpisah.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Charts (lightweight, no network dependency)
    implementation("androidx.compose.foundation:foundation")

    // Shizuku (Batch 23): akses privilege shell (shell UID via ADB pairing / root activator) TANPA
    // root permanen & TANPA request root langsung dari app ini. User install app Shizuku terpisah
    // (Play Store/GitHub) + aktifkan sendiri. Tanpa Shizuku aktif, semua fitur existing tetap 100%
    // jalan seperti biasa (graceful fallback) - lihat ShizukuManager.kt untuk detail fail-safe.
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

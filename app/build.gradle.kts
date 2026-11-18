plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// versionCode diisi otomatis oleh GitHub Actions (-PversionCode=N) setiap push.
// Build lokal (Android Studio) memakai versionCode 1.
val appVersionCode: Int = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
val appVersionName: String = "1.0.$appVersionCode"

android {
    namespace = "com.example.apkbookingfilm"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.apkbookingfilm"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GITHUB_REPO", "\"Reiii2602/APK-Booking-Film\"")
    }

    // Satu keystore yang sama untuk debug & release, supaya APK hasil update
    // bisa langsung menimpa (update) aplikasi lama tanpa uninstall.
    signingConfigs {
        create("shared") {
            storeFile = rootProject.file("keystore/cinema-booking.jks")
            storePassword = "cinemabooking"
            keyAlias = "cinemabooking"
            keyPassword = "cinemabooking"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

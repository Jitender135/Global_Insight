plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms) // Make sure this exists in your version catalog
}

android {
    namespace = "com.example.global_insights"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.global_insights"
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
    implementation ("com.google.android.material:material:1.11.0") // or latest version
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Firebase (use version catalog or hardcoded if needed)
    implementation(libs.firebase.auth.v2230)
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.firebase:firebase-firestore:25.1.2")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // SwipeRefreshLayout for Pull to Refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// Apply Firebase plugin (correct syntax for Kotlin DSL)
apply(plugin = "com.google.gms.google-services")
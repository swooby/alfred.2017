plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //id("com.google.gms.google-services")
    //id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.swooby.alfred"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.swooby.alfred2017m2"
        minSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.google.android.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

//    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
//    implementation("com.google.firebase:firebase-crashlytics")
//    implementation("com.google.firebase:firebase-analytics")

    implementation(project(":smartfoo-android-lib-core"))
}
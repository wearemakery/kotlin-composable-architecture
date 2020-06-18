plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
}

android {
    compileSdkVersion(29)

    defaultConfig {
        minSdkVersion(28)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    @Suppress("UnstableApiUsage")
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    sourceSets["androidTest"].java.srcDir("src/androidTest/kotlin")
    sourceSets["main"].java.srcDir("src/main/kotlin")
    sourceSets["test"].java.srcDir("src/test/kotlin")
}

dependencies {
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    implementation("androidx.activity:activity-ktx:1.2.0-alpha06")
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta6")
    implementation("androidx.core:core-ktx:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.0-alpha04")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("io.arrow-kt:arrow-optics:0.10.5")
    implementation(project(":composable-architecture"))
    implementation(project(":composable-architecture-android"))
    kapt("io.arrow-kt:arrow-meta:0.10.5")
    testImplementation("junit:junit:4.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.7")
    testImplementation(project(":composable-architecture-test"))
}

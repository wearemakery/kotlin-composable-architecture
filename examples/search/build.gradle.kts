plugins {
    id("shared-android")
}

android {
    defaultConfig {
        applicationId = "composablearchitecture.example.search"
    }
    @Suppress("UnstableApiUsage")
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerVersion = "1.3.70-dev-withExperimentalGoogleExtensions-20200424"
        kotlinCompilerExtensionVersion = "0.1.0-dev13"
    }
}

dependencies {
    implementation("androidx.compose:compose-runtime:0.1.0-dev13")
    implementation("androidx.ui:ui-layout:0.1.0-dev13")
    implementation("androidx.ui:ui-material:0.1.0-dev13")
    implementation("androidx.ui:ui-tooling:0.1.0-dev13")
    implementation("com.squareup.okhttp3:okhttp:4.7.2")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
}

configurations.all {
    resolutionStrategy.force("com.squareup.okhttp3:okhttp:4.7.2")
}

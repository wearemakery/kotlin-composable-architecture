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
        aidl = false
        renderScript = false
        shaders = false
    }
    composeOptions {
        kotlinCompilerExtensionVersion = kotlinComposeVersion
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:$kotlinComposeVersion")
    implementation("androidx.compose.foundation:foundation:$kotlinComposeVersion")
    implementation("androidx.compose.material:material:$kotlinComposeVersion")
    implementation("androidx.compose.ui:ui-tooling:$kotlinComposeVersion")
    implementation("androidx.compose.ui:ui:$kotlinComposeVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
}

configurations.all {
    resolutionStrategy.force("com.squareup.okhttp3:okhttp:$okhttpVersion")
}

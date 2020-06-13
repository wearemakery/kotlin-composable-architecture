import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("shared-android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.72"
}

android {
    defaultConfig {
        applicationId = "composablearchitecture.example.search"
    }
}

dependencies {
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.5.0")
    implementation("com.squareup.okhttp3:okhttp:4.7.2")
    implementation("com.squareup.retrofit2:retrofit:2.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.UnstableDefault"
}

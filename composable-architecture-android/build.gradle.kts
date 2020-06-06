plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdkVersion(29)
    compileOptions {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    sourceSets["main"].java.srcDir("src/main/kotlin")
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")
    implementation("io.arrow-kt:arrow-optics:0.10.5")
    implementation(project(":composable-architecture"))
}

plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdk = androidCompileSdkVersion
    sourceSets["main"].java.srcDir("src/main/kotlin")
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$androidxLifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$androidxLifecycleVersion")
    implementation("io.arrow-kt:arrow-optics:$arrowVersion")
    implementation(project(":composable-architecture"))
}

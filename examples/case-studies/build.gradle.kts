plugins {
    id("shared-android")
}

android {
    defaultConfig {
        applicationId = "composablearchitecture.example.casestudies"
    }
}

dependencies {
    implementation("androidx.dynamicanimation:dynamicanimation:$androidxDynamicAnimationVersion")
}

plugins {
    id("kotlin")
}

dependencies {
    implementation("io.arrow-kt:arrow-optics:$arrowVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    implementation(project(":composable-architecture"))
}

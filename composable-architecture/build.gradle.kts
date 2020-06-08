plugins {
    id("kotlin")
    id("kotlin-kapt")
}

dependencies {
    implementation("io.arrow-kt:arrow-optics:0.10.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.72")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    kaptTest("io.arrow-kt:arrow-meta:0.10.5")
    testImplementation("com.squareup.moshi:moshi-kotlin:1.9.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.7")
    testImplementation(project(":composable-architecture-test"))
}

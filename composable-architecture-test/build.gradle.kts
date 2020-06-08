plugins {
    id("kotlin")
}

dependencies {
    implementation("com.squareup.moshi:moshi-kotlin:1.9.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.72")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.7")
    implementation(project(":composable-architecture"))
}

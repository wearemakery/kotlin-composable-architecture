@file:Suppress("HasPlatformType")

import java.io.File
import java.util.Properties

private val properties = Properties().apply { File("gradle.properties").inputStream().use { load(it) } }

// Versions
val androidToolsBuildVersion = properties.getProperty("androidToolsBuildVersion")
val androidxActivityVersion = properties.getProperty("androidxActivityVersion")
val androidxAppcompatVersion = properties.getProperty("androidxAppcompatVersion")
val androidxConstraintLayoutVersion = properties.getProperty("androidxConstraintLayoutVersion")
val androidxCoreVersion = properties.getProperty("androidxCoreVersion")
val androidxDynamicAnimationVersion = properties.getProperty("androidxDynamicAnimationVersion")
val androidxEspressoVersion = properties.getProperty("androidxEspressoVersion")
val androidxJunitVersion = properties.getProperty("androidxJunitVersion")
val androidxLifecycleVersion = properties.getProperty("androidxLifecycleVersion")
val androidxRecyclerviewVersion = properties.getProperty("androidxRecyclerviewVersion")
val arrowVersion = properties.getProperty("arrowVersion")
val coroutinesVersion = properties.getProperty("coroutinesVersion")
val junitVersion = properties.getProperty("junitVersion")
val kotlinComposeVersion = properties.getProperty("kotlinComposeVersion")
val kotlinVersion = properties.getProperty("kotlinVersion")
val moshiVersion = properties.getProperty("moshiVersion")
val okhttpVersion = properties.getProperty("okhttpVersion")
val retrofitVersion = properties.getProperty("retrofitVersion")

// Android
val androidCompileSdkVersion = properties.getProperty("androidCompileSdkVersion").toInt()
val androidMinSdkVersion = properties.getProperty("androidMinSdkVersion").toInt()
val androidTargetSdkVersion = properties.getProperty("androidTargetSdkVersion").toInt()

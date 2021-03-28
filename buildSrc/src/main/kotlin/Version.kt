import org.gradle.api.Project
import java.io.File
import java.util.Properties

// Versions
val Project.androidToolsBuildVersion: String get() = props.getProperty("androidToolsBuildVersion")
val Project.androidxActivityVersion: String get() = props.getProperty("androidxActivityVersion")
val Project.androidxAppcompatVersion: String get() = props.getProperty("androidxAppcompatVersion")
val Project.androidxConstraintLayoutVersion: String get() = props.getProperty("androidxConstraintLayoutVersion")
val Project.androidxCoreVersion: String get() = props.getProperty("androidxCoreVersion")
val Project.androidxDynamicAnimationVersion: String get() = props.getProperty("androidxDynamicAnimationVersion")
val Project.androidxEspressoVersion: String get() = props.getProperty("androidxEspressoVersion")
val Project.androidxJunitVersion: String get() = props.getProperty("androidxJunitVersion")
val Project.androidxLifecycleVersion: String get() = props.getProperty("androidxLifecycleVersion")
val Project.androidxRecyclerviewVersion: String get() = props.getProperty("androidxRecyclerviewVersion")
val Project.arrowVersion: String get() = props.getProperty("arrowVersion")
val Project.coroutinesVersion: String get() = props.getProperty("coroutinesVersion")
val Project.junitVersion: String get() = props.getProperty("junitVersion")
val Project.kotlinComposeVersion: String get() = props.getProperty("kotlinComposeVersion")
val Project.kotlinVersion: String get() = props.getProperty("kotlinVersion")
val Project.moshiVersion: String get() = props.getProperty("moshiVersion")
val Project.okhttpVersion: String get() = props.getProperty("okhttpVersion")
val Project.retrofitVersion: String get() = props.getProperty("retrofitVersion")

// Android
val Project.androidCompileSdkVersion: Int get() = props.getProperty("androidCompileSdkVersion").toInt()
val Project.androidMinSdkVersion: Int get() = props.getProperty("androidMinSdkVersion").toInt()
val Project.androidTargetSdkVersion: Int get() = props.getProperty("androidTargetSdkVersion").toInt()

private val Project.props: Properties
    get() = Properties().apply { File(rootDir, "gradle.properties").inputStream().use { load(it) } }

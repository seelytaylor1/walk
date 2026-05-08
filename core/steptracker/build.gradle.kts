plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.wanderingledger.core.steptracker"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(projects.core.telemetry)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

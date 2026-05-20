plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.wanderingledger.core.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.core.steptracker)
    implementation(projects.core.telemetry)
    implementation(libs.room.ktx)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
}

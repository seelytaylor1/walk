plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.wanderingledger.core.testing"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.core.steptracker)
    implementation(libs.room.testing)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.junit)
}

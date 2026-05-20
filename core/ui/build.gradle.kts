plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.wanderingledger.core.ui"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
}

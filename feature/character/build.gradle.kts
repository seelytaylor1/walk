plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.wanderingledger.feature.character"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.ui)
}

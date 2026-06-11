plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.wanderingledger.core.haptics"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
    }
}

dependencies {
    implementation(projects.core.audio)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
}

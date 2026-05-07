plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.wanderingledger.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.wanderingledger.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.steptracker)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.feature.worldmap)
    implementation(projects.feature.town)
    implementation(projects.feature.ledger)
    implementation(projects.feature.companions)
    implementation(projects.feature.character)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.kotlinx.coroutines.android)
}

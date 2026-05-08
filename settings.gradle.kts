pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WanderingLedger"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:model")
include(":core:data")
include(":core:database")
include(":core:steptracker")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")
include(":core:telemetry")

include(":feature:worldmap")
include(":feature:town")
include(":feature:ledger")
include(":feature:companions")
include(":feature:character")
include(":feature:calibration")

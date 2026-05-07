pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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

include(":feature:worldmap")
include(":feature:town")
include(":feature:ledger")
include(":feature:companions")
include(":feature:character")

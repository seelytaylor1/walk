import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
}

// Root-level ktlint check aggregates all subproject ktlintCheck tasks.
tasks.register("ktlintCheck") {
    group = "verification"
    description = "Run ktlint on all subprojects."
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("ktlintCheck") })
}

// Root-level ktlintFormat aggregates all subproject ktlintFormat tasks.
tasks.register("ktlintFormat") {
    group = "formatting"
    description = "Auto-format Kotlin sources in all subprojects with ktlint."
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("ktlintFormat") })
}

tasks.register("jacocoTestReport") {
    group = "verification"
    description = "Placeholder coverage task until coverage gates are wired by T036."
}

subprojects {
    // Apply ktlint to every subproject.
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.2.1")
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
        }
    }

    // Wire ktlintCheck into the standard Gradle `check` lifecycle task.
    plugins.withId("com.android.application") {
        tasks.named("check") {
            dependsOn("ktlintCheck")
        }
    }
    plugins.withId("com.android.library") {
        tasks.named("check") {
            dependsOn("ktlintCheck")
        }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.named("check") {
            dependsOn("ktlintCheck")
        }
    }

    plugins.withId("com.android.application") {
        extensions.configure<BaseExtension>("android") {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }

    plugins.withId("com.android.library") {
        extensions.configure<BaseExtension>("android") {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

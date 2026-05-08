import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    id("jacoco")
    alias(libs.plugins.kotlin.compose) apply false
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
    description = "Generate aggregated JaCoCo coverage report for critical modules."
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("jacocoTestReport") })
}

val coverageModules = setOf(
    "core:data",
    "core:steptracker",
    "core:telemetry",
    "core:database",
    "core:model",
)

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
        extensions.configure<ApplicationExtension>("android") {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }

    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension>("android") {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Apply JaCoCo coverage to critical modules
    if (project.path in coverageModules) {
        plugins.apply("jacoco")
        tasks.named("testDebugUnitTest") {
            extensions.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}

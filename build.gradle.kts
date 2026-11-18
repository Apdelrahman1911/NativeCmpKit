import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidKotlinMultiplatformLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinJvm).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)
    alias(libs.plugins.composeCompiler).apply(false)
    alias(libs.plugins.composeMultiplatform).apply(false)
    alias(libs.plugins.vanniktech.maven.publish).apply(false)
}

val nativeCmpKitVersion = providers.gradleProperty("nativecmpkit.version")
    .orElse("${libs.versions.nativecmpkit.get()}-SNAPSHOT")
val verificationOnly = providers.gradleProperty("nativecmpkit.verification").orNull == "true"

allprojects {
    group = "io.github.apdelrahman1911.nativecmpkit"
    version = nativeCmpKitVersion.get()

    // A deliberate local opt-in is a safety interlock, not owner authorization.
    // Publishing still requires an explicit owner request and verified credentials/namespace.
    val allowPublishing = providers.gradleProperty("nativecmpkit.allowPublishing")
    tasks.withType<PublishToMavenRepository>().configureEach {
        doFirst {
            check(!verificationOnly && allowPublishing.orNull == "true") {
                "Remote publication requires signed artifacts and -Pnativecmpkit.allowPublishing=true."
            }
        }
    }
    // Vanniktech also exposes standalone Central deployment/release/drop tasks;
    // those may contact Central without executing PublishToMavenRepository.
    tasks.configureEach {
        if (name.contains("MavenCentral", ignoreCase = true)) {
            doFirst {
                check(!verificationOnly && allowPublishing.orNull == "true") {
                    "Maven Central operations require signed artifacts and -Pnativecmpkit.allowPublishing=true."
                }
            }
        }
    }
}

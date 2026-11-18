import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import groovy.json.JsonSlurper
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    kotlin("multiplatform") version "2.4.20" apply false
    id("com.android.kotlin.multiplatform.library") version "9.4.0" apply false
}

val manifest = JsonSlurper().parse(file("coordinates.json")) as Map<*, *>
val groupId = manifest["group"] as String
val candidateVersion = manifest["candidateVersion"] as String
val componentDependencies = (manifest["modules"] as List<*>).associate { entry ->
    val fields = entry as Map<*, *>
    (fields["artifact"] as String) to (fields["dependencies"] as List<*>).map { it as String }
}

fun permittedModules(component: String): Set<String> =
    setOf(component) + componentDependencies.getValue(component).flatMap { permittedModules(it) }

subprojects {
    val artifact = name
    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply(plugin = "com.android.kotlin.multiplatform.library")

    extensions.configure<KotlinMultiplatformExtension> {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "consumer." + artifact.replace('-', '_')
            compileSdk = 37
            minSdk = 23
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }
        jvm("desktop") { compilerOptions.jvmTarget.set(JvmTarget.JVM_17) }
        iosArm64()
        iosSimulatorArm64()
        js().browser()
        wasmJs().browser()
        sourceSets.commonMain.dependencies {
            implementation("$groupId:$artifact:$candidateVersion")
        }
    }

    val permitted = permittedModules(artifact).flatMap { listOf(it, "$it-desktop") }.toSet()
    tasks.register("verifyDesktopResolution") {
        dependsOn("compileKotlinDesktop")
        doLast {
            val runtime = configurations.getByName("desktopRuntimeClasspath")
            val resolved = runtime.incoming.resolutionResult.allComponents.mapNotNull {
                it.id as? ModuleComponentIdentifier
            }.filter { it.group == groupId }
            check(resolved.any { it.module == "$artifact-desktop" }) { "Missing $artifact-desktop" }
            check(resolved.all { it.version == candidateVersion && it.module in permitted }) {
                "Unrelated or unexpectedly versioned coordinates for $artifact: $resolved"
            }
            println("$artifact desktop Maven closure: ${resolved.map { it.module }.sorted()}")
        }
    }
}

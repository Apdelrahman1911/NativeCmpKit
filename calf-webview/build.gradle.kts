import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("compose.multiplatform")
    id("module.publication")
}

val javaFx = project.javaFxDependencies()

kotlin {
    targets.withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            compilations.getByName("main") {
                cinterops.create("nskeyvalueobserving") {
                    definitionFile = file("src/iosMain/cinterop/nskeyvalueobserving.def")
                }
            }
        }
    sourceSets.commonMain.dependencies {
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.kotlinx.coroutines.core)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.activity.compose)
        implementation(libs.kotlinx.coroutines.android)
    }

    sourceSets.desktopMain.dependencies {
        // Applications select JavaFX for the desktop they distribute to.
        javaFx.forEach { compileOnly(it) }
        implementation(libs.kotlinx.coroutines.swing)
    }

    sourceSets.desktopTest.dependencies {
        javaFx.forEach { implementation(it) }
    }

    sourceSets.webMain.dependencies {
        implementation(libs.kotlinx.browser)
    }
}

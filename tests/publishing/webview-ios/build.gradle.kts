plugins {
    kotlin("multiplatform") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

val candidateVersion = providers.gradleProperty("nativecmpkitVersion").get()

kotlin {
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "KiraWebViewConsumer"
            isStatic = true
        }
    }
    sourceSets.iosSimulatorArm64Main.dependencies {
        implementation("io.github.apdelrahman1911.nativecmpkit:kira-webview:$candidateVersion")
        implementation(compose.foundation)
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    }
}

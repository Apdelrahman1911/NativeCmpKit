import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    kotlin("multiplatform") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

val candidateVersion = providers.gradleProperty("nativecmpkitVersion").get()

plugins.withType<NodeJsPlugin> { the<NodeJsEnvSpec>().download = false }
plugins.withType<WasmNodeJsPlugin> { the<WasmNodeJsEnvSpec>().download = false }

kotlin {
    js {
        browser()
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets.commonMain.dependencies {
        implementation("io.github.apdelrahman1911.nativecmpkit:kira-webview:$candidateVersion")
        implementation("org.jetbrains.compose.foundation:foundation:1.12.0")
        implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
    }
    sourceSets.commonTest.dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        implementation(kotlin("test"))
    }
}

tasks.matching { it.name in setOf("jsBrowserTest", "wasmJsBrowserTest") }.configureEach {
    outputs.upToDateWhen { false }
}

import java.security.MessageDigest
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

val groupId = "io.github.apdelrahman1911.nativecmpkit"
val candidateVersion = providers.gradleProperty("nativecmpkitVersion").get()
val candidateRepo = file(providers.gradleProperty("nativecmpkitRepository").get())
val pomOnly = providers.gradleProperty("nativecmpkitMetadataMode").orNull == "pom"
val metadataMode = if (pomOnly) "pom" else "module"
layout.buildDirectory.set(layout.projectDirectory.dir("build/$metadataMode"))
val desktopArtifact = "kira-webview-desktop"
val jfxVersion = "19"
val os = System.getProperty("os.name").lowercase()
val arch = System.getProperty("os.arch").lowercase()
val arm64 = arch in setOf("arm64", "aarch64")
val x64 = arch in setOf("amd64", "x86_64")
val javafxClassifier = when {
    "linux" in os && arm64 -> "linux-aarch64"
    "linux" in os && x64 -> "linux"
    "mac" in os && arm64 -> "mac-aarch64"
    "mac" in os && x64 -> "mac"
    "windows" in os && x64 -> "win"
    else -> error("Unsupported desktop consumer: $os/$arch")
}

kotlin {
    jvmToolchain(17)
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    implementation("$groupId:${if (pomOnly) desktopArtifact else "kira-webview"}:$candidateVersion")
    implementation(compose.desktop.currentOs)
    listOf("base", "graphics", "controls", "media", "web", "swing").forEach {
        implementation("org.openjfx:javafx-$it:$jfxVersion:$javafxClassifier")
    }
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    if ("linux" in os) environment("SKIKO_RENDER_API", "SOFTWARE")
    testLogging { exceptionFormat = TestExceptionFormat.FULL }
    doFirst {
        val artifact = configurations.testRuntimeClasspath.get().incoming.artifacts.artifacts
            .filter { (it.id.componentIdentifier as? ModuleComponentIdentifier)?.group == groupId }
            .single { it.file.extension == "jar" }
        val id = artifact.id.componentIdentifier as ModuleComponentIdentifier
        check(id.module == desktopArtifact && id.version == candidateVersion)
        val staged = candidateRepo.resolve("${groupId.replace('.', '/')}/$desktopArtifact/$candidateVersion/$desktopArtifact-$candidateVersion.jar")
        val digest = MessageDigest.getInstance("SHA-256").digest(staged.readBytes())
        check(MessageDigest.getInstance("SHA-256").digest(artifact.file.readBytes()).contentEquals(digest))
        systemProperty("kira.webview.sha256", digest.joinToString("") { "%02x".format(it) })
    }
    outputs.upToDateWhen { false }
}

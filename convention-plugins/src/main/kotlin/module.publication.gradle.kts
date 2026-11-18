import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.vanniktech.maven.publish")
}

val component = project.name
val repositoryRoot = rootProject.layout.projectDirectory
val noticeFiles = files(repositoryRoot.file("LICENSE"), repositoryRoot.file("NOTICE"), repositoryRoot.file("THIRD_PARTY_NOTICES.md"))
val publicationNotices = tasks.register<Zip>("publicationNotices") {
    archiveClassifier.set("notices")
    from(noticeFiles)
    from(repositoryRoot.dir("licenses")) {
        into("licenses")
        exclude("native-cargo/**")
    }
    if (component in setOf("kira-file-picker", "kira-share")) {
        from(rootProject.layout.buildDirectory.dir("native-notices/$component")) {
            into("native-dependencies")
        }
    }
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<Jar>().configureEach {
    from(noticeFiles) { into("META-INF/nativecmpkit/$component") }
    if (name.endsWith("JavadocJar", ignoreCase = true)) {
        from(repositoryRoot.file("README.md"))
    }
}
tasks.withType<Zip>().matching { it.name.endsWith("Aar") }.configureEach {
    from(noticeFiles) { into("META-INF/nativecmpkit/$component") }
}
publishing.publications.withType<MavenPublication>().configureEach {
    artifact(publicationNotices)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)
    // CI uses an isolated Maven Local repository to inspect unsigned candidates.
    // The root build rejects all remote publishing in this mode.
    if (providers.gradleProperty("nativecmpkit.verification").orNull != "true") {
        signAllPublications()
    }

    coordinates(group.toString(), component, version.toString())

    pom {
        name.set("NativeCmpKit $component")
        description.set("The $component component of NativeCmpKit for Kotlin Multiplatform.")
        url.set("https://github.com/Apdelrahman1911/NativeCmpKit")

        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://opensource.org/licenses/Apache-2.0")
            }
        }
        developers {
            developer {
                id.set("Apdelrahman1911")
                name.set("Abdelrahman Fahmy")
                url.set("https://github.com/Apdelrahman1911")
            }
        }
        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/Apdelrahman1911/NativeCmpKit/issues")
        }
        scm {
            connection.set("scm:git:https://github.com/Apdelrahman1911/NativeCmpKit.git")
            developerConnection.set("scm:git:ssh://git@github.com/Apdelrahman1911/NativeCmpKit.git")
            url.set("https://github.com/Apdelrahman1911/NativeCmpKit")
        }
    }
}

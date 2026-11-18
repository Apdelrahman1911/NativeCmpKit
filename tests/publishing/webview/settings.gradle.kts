pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}

rootProject.name = "kira-webview-maven-consumer"
val candidate = File(providers.gradleProperty("nativecmpkitRepository").get()).canonicalFile
require(candidate.isDirectory)
val pomOnly = providers.gradleProperty("nativecmpkitMetadataMode").orNull == "pom"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    url = candidate.toURI()
                    metadataSources {
                        if (pomOnly) {
                            mavenPom()
                            ignoreGradleMetadataRedirection()
                        } else {
                            gradleMetadata()
                        }
                    }
                }
            }
            filter { includeGroup("io.github.apdelrahman1911.nativecmpkit") }
        }
        google()
        mavenCentral()
    }
}

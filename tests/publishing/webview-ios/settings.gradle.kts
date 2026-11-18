pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}

rootProject.name = "kira-webview-ios-consumer"

val candidate = File(providers.gradleProperty("nativecmpkitRepository").get()).canonicalFile
require(candidate.isDirectory)

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository { maven { url = candidate.toURI() } }
            filter { includeGroup("io.github.apdelrahman1911.nativecmpkit") }
        }
        google()
        mavenCentral()
    }
}

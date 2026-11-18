import groovy.json.JsonSlurper

pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}

rootProject.name = "nativecmpkit-maven-consumers"

val manifest = JsonSlurper().parse(file("coordinates.json")) as Map<*, *>
val modules = manifest["modules"] as List<*>
modules.forEach { include((it as Map<*, *>)["artifact"] as String) }

val candidateRepository = File(providers.gradleProperty("nativecmpkitRepository").get()).canonicalFile
require(candidateRepository.isDirectory)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven { url = candidateRepository.toURI() }
            }
            filter { includeGroup(manifest["group"] as String) }
        }
        google()
        mavenCentral()
    }
}

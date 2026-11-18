plugins {
    kotlin("jvm") version "2.4.20"
    application
}

val releaseVersion = providers.gradleProperty("libraryVersion").get()
kotlin { jvmToolchain(17) }
dependencies {
    implementation("io.github.apdelrahman1911.nativecmpkit:kira-io:$releaseVersion")
}
application { mainClass.set("ConsumerKt") }

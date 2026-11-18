rootProject.name = "NativeCmpKit"

pluginManagement {
    includeBuild("convention-plugins")
    repositories {
        google {
            mavenContent {
                releasesOnly()
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    // Artifact modules
    ":kira-core",
    ":kira-ui",
    ":kira-webview",
    ":kira-cupertino-icons",
    ":kira-navigation",
    ":kira-io",
    ":kira-file-picker",
    ":kira-file-picker-coil",
    ":kira-permissions",
    ":kira-permissions:core",
    ":kira-permissions:camera",
    ":kira-permissions:gallery",
    ":kira-permissions:location",
    ":kira-permissions:background-location",
    ":kira-permissions:bluetooth",
    ":kira-permissions:contacts",
    ":kira-permissions:calendar",
    ":kira-permissions:notifications",
    ":kira-permissions:wifi",
    ":kira-permissions:microphone",
    ":kira-geo",
    ":kira-maps",
    ":kira-camera-picker",
    ":kira-share",
    // Sample modules
    ":sample:android",
    ":sample:desktop",
    ":sample:web-js",
    ":sample:web-wasm",
    ":sample:common",
)

// Give each permission leaf a unique Maven artifact ID.
project(":kira-permissions:core").name = "kira-permissions-core"
project(":kira-permissions:camera").name = "kira-permissions-camera"
project(":kira-permissions:gallery").name = "kira-permissions-gallery"
project(":kira-permissions:location").name = "kira-permissions-location"
project(":kira-permissions:background-location").name = "kira-permissions-background-location"
project(":kira-permissions:bluetooth").name = "kira-permissions-bluetooth"
project(":kira-permissions:contacts").name = "kira-permissions-contacts"
project(":kira-permissions:calendar").name = "kira-permissions-calendar"
project(":kira-permissions:notifications").name = "kira-permissions-notifications"
project(":kira-permissions:wifi").name = "kira-permissions-wifi"
project(":kira-permissions:microphone").name = "kira-permissions-microphone"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

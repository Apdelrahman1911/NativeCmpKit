plugins {
    id("compose.multiplatform")
    id("module.publication")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.compose.foundation)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.appcompat)
        implementation(libs.lifecycle.extensions)
        implementation(libs.play.services.location)
    }
}

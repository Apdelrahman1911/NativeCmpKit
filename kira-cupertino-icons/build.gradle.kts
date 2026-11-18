plugins {
    id("compose.multiplatform")
    id("module.publication")
}

compose.resources {
    packageOfResClass = "io.github.apdelrahman1911.kira.kira_cupertino_icons.generated.resources"
}

kotlin {
    sourceSets.desktopTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.compose.ui.test)
        implementation(compose.desktop.currentOs)
    }

    sourceSets.commonMain.dependencies {
        api(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.components.resources)
    }
}

plugins {
    id("com.android.application") version "9.4.0"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

val candidateVersion = providers.gradleProperty("nativecmpkitVersion").get()

android {
    namespace = "consumer.kira.webview"
    compileSdk = 37
    defaultConfig {
        applicationId = "consumer.kira.webview"
        minSdk = 23
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("io.github.apdelrahman1911.nativecmpkit:kira-webview:$candidateVersion")
    implementation(compose.foundation)
    implementation("androidx.activity:activity-compose:1.13.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("junit:junit:4.13.2")
}

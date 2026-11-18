plugins {
    id("compose.multiplatform")
    id("module.publication")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.kiraPermissions.kiraPermissionsCore)
        api(projects.kiraPermissions.kiraPermissionsCamera)
        api(projects.kiraPermissions.kiraPermissionsGallery)
        api(projects.kiraPermissions.kiraPermissionsLocation)
        api(projects.kiraPermissions.kiraPermissionsBackgroundLocation)
        api(projects.kiraPermissions.kiraPermissionsBluetooth)
        api(projects.kiraPermissions.kiraPermissionsContacts)
        api(projects.kiraPermissions.kiraPermissionsCalendar)
        api(projects.kiraPermissions.kiraPermissionsNotifications)
        api(projects.kiraPermissions.kiraPermissionsWifi)
        api(projects.kiraPermissions.kiraPermissionsMicrophone)
    }
}

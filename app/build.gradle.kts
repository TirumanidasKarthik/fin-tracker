plugins {
    id("com.android.application")
}

android {
    namespace = "com.fintracker.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fintracker.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}


plugins {
    id("com.android.application")
}

val releaseKeystorePath = providers.environmentVariable("ANDROID_RELEASE_KEYSTORE_FILE").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_RELEASE_KEY_ALIAS").orNull
val releaseKeystorePassword = providers.environmentVariable("ANDROID_RELEASE_KEYSTORE_PASSWORD").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseKeyAlias,
    releaseKeystorePassword,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

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

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}


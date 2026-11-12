plugins {
    id("com.android.application")
}

val run = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.kyfreq.kuroos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kyfreq.kuroos"
        minSdk = 26
        targetSdk = 34
        versionCode = run
        versionName = "0.1.$run"
    }

    // One fixed key, so every new build installs as an update over the old one.
    signingConfigs {
        create("kuro") {
            storeFile = file("kuro.keystore")
            storePassword = "kuroos"
            keyAlias = "kuro"
            keyPassword = "kuroos"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("kuro")
        }
        debug {
            signingConfig = signingConfigs.getByName("kuro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

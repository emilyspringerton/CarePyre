plugins {
    id("com.android.application")
}

android {
    namespace = "org.carepyre.sip"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.carepyre.sip"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

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

dependencies {
    // zxing-android-embedded -- CAREPYRE-42143124's own "qr code scan feature to configure" ask.
    // The real, standard, widely-used wrapper around ZXing core for Android (IntentIntegrator/
    // IntentResult, a ready-made CaptureActivity) rather than hand-rolling camera+decode logic --
    // matches this monorepo's own "reuse a real, established library over reinventing it" default
    // for anything outside PARENA's own dogfooded surface (PARENA has no camera/QR primitives of
    // its own; this is real platform-integration glue, not core SIP logic). Resolved from
    // mavenCentral(), already configured in settings.gradle.kts.
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}

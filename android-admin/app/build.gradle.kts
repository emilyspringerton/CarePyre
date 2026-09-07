plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// CP-WHITELABEL-1: "it needs to be both white labeled first then made into carepyre." One
// codebase, one WebView-wrapper Activity (MainActivity.kt) -- everything tenant-specific (app
// name/icon, applicationId, and which console URL it points at) lives in these two product
// flavors, not in forked code. Adding a THIRD tenant later means adding a THIRD flavor block
// here, never copying this module.
android {
    namespace = "pro.iduna.admin"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    flavorDimensions += "brand"
    productFlavors {
        // The white-label default -- what this app is before any tenant configures it. Points
        // at whatever base URL an operator deploys their own IDUNA_PRO console.html to.
        create("generic") {
            dimension = "brand"
            applicationId = "pro.iduna.admin"
            resValue("string", "app_name", "IDUNA Pro Admin")
            buildConfigField("String", "CONSOLE_URL", "\"https://example.invalid/console.html\"")
        }
        // CarePyre is the first real tenant of the white-label template above -- same code,
        // its own applicationId/name/URL, nothing forked.
        create("carepyre") {
            dimension = "brand"
            applicationId = "org.carepyre.admin"
            resValue("string", "app_name", "CarePyre Admin")
            buildConfigField("String", "CONSOLE_URL", "\"https://carepyre.org/console.html\"")
        }
    }

    buildFeatures {
        buildConfig = true
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}

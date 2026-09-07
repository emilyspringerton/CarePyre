pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// CP-WHITELABEL-1 (founder real-time: "it needs to be both white labeled first then made into
// carepyre"): this is the real, general IDUNA Pro Admin app -- ONE codebase, ONE :app module,
// two product flavors (see app/build.gradle.kts). "generic" is the white-label default anyone
// could ship; "carepyre" is CarePyre's own branded build of that exact same code, not a fork.
rootProject.name = "IdunaProAdmin"
include(":app")

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("ci") {
    group = "verification"
    description = "Runs the complete build validation suite used by CI."
    dependsOn(
        ":app:test",
        ":app:compileDebugAndroidTestKotlin",
        ":app:lintDebug",
        ":app:lintRelease",
        ":app:assembleDebug",
        ":app:assembleRelease"
    )
}

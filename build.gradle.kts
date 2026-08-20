// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

val supportedAapt2Hosts = listOf("linux", "osx", "windows")
val agpVersion = libs.versions.agp.get()
val aapt2Version = libs.versions.aapt2.get()

check(aapt2Version.substringBeforeLast('-') == agpVersion) {
    "AAPT2 version $aapt2Version must match Android Gradle Plugin $agpVersion. " +
        "Use the AAPT2 build published by that AGP release."
}

val aapt2HostArtifacts by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    description = "AAPT2 binaries covered by dependency verification for supported build hosts."
}

dependencies {
    supportedAapt2Hosts.forEach { host ->
        add(
            aapt2HostArtifacts.name,
            "com.android.tools.build:aapt2:$aapt2Version:$host"
        )
    }
}

val verifyAapt2HostArtifacts by tasks.registering {
    group = "verification"
    description = "Verifies AAPT2 binaries for Linux, macOS, and Windows hosts."
    inputs.files(aapt2HostArtifacts)

    doLast {
        val resolvedNames = aapt2HostArtifacts.files.mapTo(mutableSetOf()) { it.name }
        val expectedNames = supportedAapt2Hosts.mapTo(mutableSetOf()) { host ->
            "aapt2-$aapt2Version-$host.jar"
        }

        check(resolvedNames == expectedNames) {
            "Resolved AAPT2 host artifacts $resolvedNames; expected $expectedNames."
        }
    }
}

tasks.register("ci") {
    group = "verification"
    description = "Runs the complete build validation suite used by CI."
    dependsOn(
        verifyAapt2HostArtifacts,
        ":app:test",
        ":app:compileDebugAndroidTestKotlin",
        ":app:lintDebug",
        ":app:lintRelease",
        ":app:assembleDebug",
        ":app:assembleRelease"
    )
}

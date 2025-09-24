plugins {
    // Using the version from libs.versions.toml
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.5" apply false
}
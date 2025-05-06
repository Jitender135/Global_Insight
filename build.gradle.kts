// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms) apply false
    // If you're using Kotlin too:
    // alias(libs.plugins.kotlin.android) apply false
}

// No need for buildscript block if you're using Version Catalog for plugins
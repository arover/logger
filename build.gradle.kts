// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    //extra["compose_version"] = "1.3.3"
    dependencies {
    }
}

plugins {
    // https://developer.android.com/build/releases/gradle-plugin
    id("com.android.application") version "8.0.1" apply false
    id("com.android.library") version "8.0.1" apply false
    //https://plugins.gradle.org/plugin/org.jetbrains.kotlin.android
    id("org.jetbrains.kotlin.android") version "1.8.20" apply false
    id("org.jetbrains.kotlin.kapt") version "1.8.20" apply false
    //https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
    id("org.jetbrains.kotlin.jvm") version "1.8.20" apply false
}
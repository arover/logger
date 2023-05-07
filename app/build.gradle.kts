plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.arover.logger.testapp"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.arover.logger"

        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        versionCode = 1_03_00
        versionName = "1.3.0"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
        lint {
            checkReleaseBuilds = false
        }
    }

    buildFeatures {
        // Enables Jetpack Compose for this module
        // https://developer.android.com/reference/tools/gradle-api/7.0/com/android/build/api/dsl/BuildFeatures
//        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
//    composeOptions {
//        // https://developer.android.com/jetpack/androidx/releases/compose-compiler
//        kotlinCompilerExtensionVersion = "1.4.6"
//    }
    packagingOptions {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt")
            pickFirsts += listOf("META-INF/versions/9/previous-compilation-data.bin")
        }
    }
}

dependencies {

    implementation(libs.x.appcompat)
    implementation(libs.x.constraintlayout)
// https://mvnrepository.com/artifact/io.reactivex.rxjava3/rxjava
    implementation("io.reactivex.rxjava3:rxjava:3.1.6")
    // https://mvnrepository.com/artifact/io.reactivex.rxjava3/rxandroid
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")


    implementation(project(":log"))

    testImplementation(libs.junit4)
    androidTestImplementation(libs.x.test.ext.junit)
    androidTestImplementation(libs.x.test.espresso.core)
}
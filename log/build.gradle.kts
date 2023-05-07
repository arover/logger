plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    //https://vanniktech.github.io/gradle-maven-publish-plugin/central/#publishing-releases
    //https://central.sonatype.org/publish/publish-gradle/#releasing-to-central
    //https://central.sonatype.org/publish/release/
    id("com.vanniktech.maven.publish") version "0.25.2"
}

android {
    namespace = "com.arover.logger"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {

        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

//        versionCode = 3
//        versionName = "2.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true

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
}

dependencies {
    implementation(libs.x.annotation)
    testImplementation(libs.junit4)
}


plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    `maven-publish`
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    android {
        publishLibraryVariants("release")

        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "dragcontext"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.foundation)
                // Workaround as per https://youtrack.jetbrains.com/issue/KT-41821
                implementation("org.jetbrains.kotlinx:atomicfu:0.17.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.susumunoda.compose.dragcontext"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}

group = "com.susumunoda.compose"
version = "1.0"
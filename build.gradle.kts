plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.library").version("8.2.0-alpha15").apply(false)
    id("org.jetbrains.compose").version("1.5.10-beta02").apply(false)
    kotlin("multiplatform").version("1.8.21").apply(false)
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

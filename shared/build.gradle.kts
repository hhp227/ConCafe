import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kmp.nativecoroutines)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = JvmTarget.JVM_11.target
            }
        }
    }
    
    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.koin.core)
            implementation(libs.kmp.nativecoroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.kmp.nativecoroutines.core)
            implementation(libs.ktor.client.darwin)
        }
        androidMain.dependencies {
            implementation(libs.google.play.services.ads)
            implementation(libs.ktor.client.android)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.jna)
        }
    }
    targets.all {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-opt-in=kotlin.experimental.ExperimentalObjCName")
            }
        }
    }
}

android {
    namespace = "com.hhp227.concafe.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kmp.nativecoroutines.ksp)
    add("kspIosX64", libs.kmp.nativecoroutines.ksp)
    add("kspIosArm64", libs.kmp.nativecoroutines.ksp)
    add("kspIosSimulatorArm64", libs.kmp.nativecoroutines.ksp)
}

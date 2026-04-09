import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.internal.os.OperatingSystem

val javafxPlatform = when {
    OperatingSystem.current().isWindows -> "win"
    OperatingSystem.current().isMacOsX -> "mac"
    else -> "linux"
}

val androidGoogleMapsXml = file("src/androidMain/res/values/google_maps.xml")
val googleMapsJavascriptApiKey = if (androidGoogleMapsXml.exists()) {
    val xmlContent = androidGoogleMapsXml.readText()
    val keyPattern = Regex("""<string\s+name=["']google_maps_api_key["'][^>]*>([^<]+)</string>""")
    val matchedKey = keyPattern.find(xmlContent)?.groupValues?.get(1)?.trim().orEmpty()
    matchedKey
} else {
    ""
}

val appVersionName = "1.02"
val desktopPackageVersion = "1.2.0"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.google.services)
}

compose.resources {
    publicResClass = true
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = JvmTarget.JVM_11.target
            }
        }
    }
    
    jvm()
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.compose.material.icons.extended)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
            implementation(libs.google.play.services.maps)
            implementation(libs.google.play.services.ads)
            implementation(libs.google.maps.compose)
            implementation(libs.firebase.auth.ktx)
            implementation(libs.firebase.messaging.ktx)
            implementation(libs.kakao.user)
            implementation(libs.coil.compose)
            implementation(libs.androidx.core.splashscreen)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("${libs.javafx.base.get().module}:${libs.versions.javafx.get()}:$javafxPlatform")
            implementation("${libs.javafx.graphics.get().module}:${libs.versions.javafx.get()}:$javafxPlatform")
            implementation("${libs.javafx.controls.get().module}:${libs.versions.javafx.get()}:$javafxPlatform")
            implementation("${libs.javafx.swing.get().module}:${libs.versions.javafx.get()}:$javafxPlatform")
            implementation("${libs.javafx.web.get().module}:${libs.versions.javafx.get()}:$javafxPlatform")
            implementation("${libs.javafx.media.get().module}:${libs.versions.javafx.get()}:$javafxPlatform")
        }
    }
}

android {
    namespace = "com.hhp227.concafe"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.hhp227.concafe"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 4
        versionName = appVersionName
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.navigation.compose.android)
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.hhp227.concafe.MainKt"
        if (googleMapsJavascriptApiKey.isNotBlank()) {
            jvmArgs("-Dgoogle.maps.api.key=$googleMapsJavascriptApiKey")
        }
        jvmArgs("-Dapp.version=$appVersionName")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.hhp227.concafe"
            packageVersion = desktopPackageVersion
        }
    }
}

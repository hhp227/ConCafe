# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# -------------------------------------------------------------------
# Firestore (toObject) reflection safety for app models
# -------------------------------------------------------------------

# Keep annotation/metadata attributes used by Firestore and Kotlin reflection.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlin
-keep class kotlin.Metadata { *; }

# Koin (DI)
-keep class org.koin.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Serialization
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Google Play Services (Maps / Ads)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Kakao SDK
-keep class com.kakao.** { *; }
-dontwarn com.kakao.**

# WebView (혹시 JS bridge 사용 시)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Model / DTO (필요 시)
-keep class com.hhp227.concafe.domain.model.** { *; }

# enum 유지
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn org.slf4j.impl.StaticLoggerBinder
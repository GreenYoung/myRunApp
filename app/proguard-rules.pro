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

# AMap SDK optional runtime integrations are not packaged in the selected AAR.
-dontwarn com.amap.ams.gnss.GnssSoftLocator
-dontwarn net.jafama.FastMath

# AMap 3D Map/Search/Location SDK uses native code and reflection internally.
# Keep these classes stable in release builds to avoid map-page runtime crashes.
-keep class com.amap.** { *; }
-keep class com.autonavi.** { *; }
-keep class com.loc.** { *; }
-keep class com.autonavi.amap.mapcore.** { *; }
-keep class com.autonavi.base.** { *; }
-keep class com.autonavi.extra.** { *; }
-dontwarn com.amap.**
-dontwarn com.autonavi.**
-dontwarn com.loc.**

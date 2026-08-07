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

# --- Sketchpad: Room & Moshi keep rules (NEW hardening, added by the safe-feature port) ---
# Baseline d7e969d had no keep rules and the refactored range did not touch this file;
# these are the canonical rules from the Room and Moshi documentation.

# Room: generated *_Impl classes are located via reflection at runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keepclassmembers class * { @androidx.room.* <methods>; }
-dontwarn androidx.room.paging.**

# Moshi: @JsonClass(generateAdapter = true) models are referenced by generated adapters.
-keep @com.squareup.moshi.JsonClass class *
-keepclassmembers class * { @com.squareup.moshi.Json *; }
-keepclasseswithmembers class * { @com.squareup.moshi.FromJson <methods>; }
-keepclasseswithmembers class * { @com.squareup.moshi.ToJson <methods>; }


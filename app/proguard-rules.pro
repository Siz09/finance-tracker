# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ── Room ─────────────────────────────────────────────────────────────────────
# Keep all entity/model classes so Room reflection and migrations work in release.
-keep class com.example.data.model.** { *; }
-keep class * extends androidx.room.RoomDatabase
# Keep generated Room implementations
-keep class * extends androidx.room.RoomDatabase_Impl { *; }

# ── Coil ─────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── ML Kit ───────────────────────────────────────────────────────────────────
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

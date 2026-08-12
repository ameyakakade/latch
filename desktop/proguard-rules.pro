# ProGuard rules for the Latch desktop release build.
#
# Everything kept here is reached by reflection, JNI, or a service loader, so
# ProGuard cannot see the reference and would otherwise strip it. Each failure
# mode is runtime-only and does NOT reproduce under `gradle run`, which makes
# these rules easy to get wrong and painful to debug -- always smoke-test the
# packaged exe after changing them.

# ---------------------------------------------------------------------------
# Global shrink / obfuscation options
# ---------------------------------------------------------------------------
# Flatten remaining classes into a single package -- smaller jar, fewer dirs.
-repackageclasses 'latch'
# Keep source file name and line numbers so obfuscated stack traces are
# still useful for crash reports (class/method names will be scrambled).
-keepattributes SourceFile,LineNumberTable
# Silence notes about duplicate class definitions from bundled dependencies.
-dontnote **

# ---------------------------------------------------------------------------
# Room + AndroidX SQLite
# ---------------------------------------------------------------------------
# Room instantiates the KSP-generated *_Impl classes reflectively by name.
-keep class com.vinnovateit.latch.core.data.** { *; }
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
# The bundled SQLite driver loads a native library through JNI; the native method
# bindings must keep their exact names.
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------------------------------------------------------------------------
# JNA (DPAPI credentials + registry autostart)
# ---------------------------------------------------------------------------
# JNA maps Java interfaces onto native functions via dynamic proxies and matches
# on member names, so nothing under com.sun.jna may be renamed or removed.
-keep class com.sun.jna.** { *; }
-keep interface com.sun.jna.** { *; }
-keep class * implements com.sun.jna.Library { *; }
-keep class * extends com.sun.jna.Structure { *; }

# ---------------------------------------------------------------------------
# OSHI (per-interface byte counters)
# ---------------------------------------------------------------------------
# OSHI reaches Windows APIs through JNA and reads platform classes reflectively.
-keep class oshi.** { *; }
-dontwarn oshi.**

# ---------------------------------------------------------------------------
# kotlinx.serialization (settings + credential blob)
# ---------------------------------------------------------------------------
# Generated serializers are looked up reflectively from the companion.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.vinnovateit.latch.**$$serializer { *; }
-keepclassmembers class com.vinnovateit.latch.** {
    *** Companion;
}
-keepclasseswithmembers class com.vinnovateit.latch.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Compose / Skiko
# ---------------------------------------------------------------------------
-dontwarn org.jetbrains.skia.**
-dontwarn org.jetbrains.skiko.**

# ---------------------------------------------------------------------------
# Logging + misc
# ---------------------------------------------------------------------------
# SLF4J binds its provider through ServiceLoader.
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**
-keep class * implements java.util.logging.Formatter { *; }

# Coroutines' internal atomics and the debug agent are reflective.
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory { *; }
# Loaded via META-INF/services.
-keep class * implements kotlinx.coroutines.internal.MainDispatcherFactory { *; }

# Keep the entry point.
-keep class com.vinnovateit.latch.desktop.MainKt { *; }

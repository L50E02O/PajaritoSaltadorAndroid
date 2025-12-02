# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep game classes
-keep class com.pajaritosaltador.game.** { *; }

# Keep Kotlin metadata
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }

# Keep Android classes
-keep class androidx.** { *; }


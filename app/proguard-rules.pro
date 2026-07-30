# Project-specific ProGuard rules

# Keep Data Models to prevent Gson/Room from breaking due to reflection
-keep class com.learn_japanese_with_music.features.lyrics.model.** { *; }
-keep class com.learn_japanese_with_music.features.vocabulary.model.** { *; }
-keep class com.learn_japanese_with_music.core.data.database.** { *; }

# Gson specific rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }

# Keep SerializedName annotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room specific rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

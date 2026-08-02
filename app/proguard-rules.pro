# ----------------------------------------------------------------------------------
# Project-Specific Rules
# ----------------------------------------------------------------------------------

# Keep Data Models to prevent Gson/Room from breaking due to reflection
-keep class com.learn_japanese_with_music.features.lyrics.model.** { *; }
-keep class com.learn_japanese_with_music.features.vocabulary.model.** { *; }
-keep class com.learn_japanese_with_music.core.data.database.** { *; }
-keep class com.learn_japanese_with_music.BuildConfig { *; }

# ----------------------------------------------------------------------------------
# Gson Rules (Reinforced for TypeToken)
# ----------------------------------------------------------------------------------
-keepattributes Signature, EnclosingMethod, InnerClasses, *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keep public class * extends com.google.gson.reflect.TypeToken

# ----------------------------------------------------------------------------------
# Retrofit 2 Rules
# ----------------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# ----------------------------------------------------------------------------------
# OkHttp 3 / Okio Rules
# ----------------------------------------------------------------------------------
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ----------------------------------------------------------------------------------
# Google Gemini (GenerativeAI) Rules
# ----------------------------------------------------------------------------------
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ----------------------------------------------------------------------------------
# Sudachi (Japanese Tokenizer) Rules
# ----------------------------------------------------------------------------------
-keep class com.worksap.nlp.sudachi.** { *; }
-dontwarn com.worksap.nlp.sudachi.**

# ----------------------------------------------------------------------------------
# Jsoup Rules (for Scraping)
# ----------------------------------------------------------------------------------
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ----------------------------------------------------------------------------------
# Room Rules
# ----------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------------
# Kotlin Coroutines Rules
# ----------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ----------------------------------------------------------------------------------
# Reflection and Type Support
# ----------------------------------------------------------------------------------
-keep class java.lang.reflect.Type
-keep class java.lang.reflect.ParameterizedType
-keep class java.lang.reflect.GenericArrayType
-keep class java.lang.reflect.WildcardType
-keep class java.lang.reflect.TypeVariable

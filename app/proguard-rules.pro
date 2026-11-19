# Keep MAX entry points and service components intact in R8 release builds.
-keep class com.example.MaxApplication { *; }
-keep class com.example.MainActivity { *; }
-keep class com.example.service.MaxAssistantService { *; }

# Preserve Kotlin metadata and Coroutines internals cleanly.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepclassmembers class kotlinx.coroutines.** { *; }

# Strip verbose/debug/info platform logs in release APK builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

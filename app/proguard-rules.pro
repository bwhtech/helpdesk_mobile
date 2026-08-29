# Keep DTOs intact for Gson reflection. All remote DTOs and domain models that
# cross the wire or get persisted by Room need to keep field names.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class io.github.kaulith.helpdeskanalytics.data.remote.dto.** { *; }
-keep,allowobfuscation,allowshrinking class io.github.kaulith.helpdeskanalytics.data.local.database.entities.** { *; }
-keep,allowobfuscation,allowshrinking class io.github.kaulith.helpdeskanalytics.domain.model.** { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# R8 full mode drops generic signatures from types it is not told to keep, and
# Retrofit reads them to resolve what a suspend function returns.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers enum * { *; }

# Room
-keep class androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Koin
-keep class org.koin.** { *; }
-keepclassmembers class * {
    public <init>(...);
}

# Compose runtime keeps itself; nothing extra needed.

# Coil
-dontwarn coil.**

# Firebase already ships consumer rules.

# Strip debug logging from release builds; Log.e survives for crash diagnosis.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Project specific ProGuard / R8 rules
# Giữ lại thông tin dòng và thuộc tính annotation cho crash report và reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Giữ nguyên cấu trúc các Data Model dùng cho Firebase Firestore, JSON và reflection
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** {
    <fields>;
    <init>(...);
    *** get*();
    *** set*(...);
    *** is*();
}

# Giữ nguyên ViewModel và Application
-keep class com.example.viewmodel.** { *; }
-keep class com.example.MainApplication { *; }
-keep class com.example.MainActivity { *; }

# Firebase SDK & Google Play Services
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidX Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Jetpack Compose
-dontwarn androidx.compose.**
